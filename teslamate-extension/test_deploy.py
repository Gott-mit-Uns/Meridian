import importlib.util
import pathlib
import unittest
from unittest.mock import patch

spec=importlib.util.spec_from_file_location('deployment',pathlib.Path(__file__).with_name('deploy.py'))
deployment=importlib.util.module_from_spec(spec);spec.loader.exec_module(deployment)

class DeploymentTest(unittest.TestCase):
    def original(self):
        return {'Id':'old-id','Name':'/teslamate-teslamateapi-1','Config':{'Env':['EXAMPLE_CONFIG=synthetic'], 'Cmd':['./app'],'Labels':{'com.docker.compose.service':'teslamateapi'}},
                'HostConfig':{'NetworkMode':'test-network','PortBindings':{'8080/tcp':[{'HostIp':'127.0.0.1','HostPort':'8081'}]},'RestartPolicy':{'Name':'unless-stopped'}},
                'NetworkSettings':{'Networks':{'test-network':{'Aliases':['teslamateapi']}}},'Image':'old-image'}
    def test_validation_does_not_touch_the_primary_container(self):
        requests=[]
        def engine(method,path,body=None):
            requests.append((method,path,body))
            if path=='/containers/meridian-api-validation/json':raise deployment.DockerFailure(404)
            return self.original() if method=='GET' else None
        with patch.object(deployment,'engine',engine),patch.object(deployment,'verify'),patch.object(deployment,'prepare_compose',return_value=(lambda:None,lambda:None)),patch('sys.argv',['deploy.py','validate']):deployment.main()
        self.assertFalse(any('/stop' in p or '/rename' in p for _,p,_ in requests))
        created=next(b for m,p,b in requests if '/create?' in p)
        self.assertNotIn('com.docker.compose.service',created['Labels'])
        self.assertEqual(self.original()['Config']['Env'],created['Env'])
    def simulate_activation_failure(self,fail_path,interruption=False):
        requests=[];old=self.original();renamed=False;new_created=False
        def engine(method,path,body=None):
            nonlocal renamed,new_created
            requests.append((method,path))
            if path=='/containers/meridian-api-validation/json':return {'Image':'validated-image','Config':{'Labels':{'meridian.validation':'true'}}}
            if path.startswith('/images/'):return {'Id':'validated-image'}
            if method=='GET' and '-rollback-' in path:raise deployment.DockerFailure(404)
            if path=='/containers/teslamate-teslamateapi-1/json':
                if new_created:return {'Id':'new-id','Image':'validated-image'}
                if renamed:raise deployment.DockerFailure(404)
                return old
            if path=='/containers/old-id/json':return {**old,'Name':'/backup' if renamed else old['Name']}
            if fail_path in path:
                if interruption:raise KeyboardInterrupt()
                raise RuntimeError('synthetic failure')
            if '/rename?name=teslamate-teslamateapi-1-rollback-' in path:renamed=True
            if '/create?' in path:new_created=True
        with patch.object(deployment,'engine',engine),patch.object(deployment,'verify'),patch.object(deployment,'prepare_compose',return_value=(lambda:None,lambda:None)),patch('sys.argv',['deploy.py','activate']):
            if interruption:
                with self.assertRaises(KeyboardInterrupt):deployment.main()
            else:
                with self.assertRaisesRegex(RuntimeError,'previous API restored'):deployment.main()
        self.assertEqual(('POST','/containers/old-id/start'),requests[-1])
    def test_start_failure_restores_original_service(self):self.simulate_activation_failure('/containers/teslamate-teslamateapi-1/start')
    def test_rename_failure_also_restarts_original_service(self):self.simulate_activation_failure('/rename?name=teslamate-teslamateapi-1-rollback-')
    def test_interruption_also_restores_original_service(self):self.simulate_activation_failure('/containers/teslamate-teslamateapi-1/start',True)
    def test_success_disables_backup_restart_and_keeps_new_policy(self):
        old=self.original();old['HostConfig']['RestartPolicy']={'Name':'always'};requests=[]
        def engine(method,path,body=None):
            requests.append((method,path,body))
            if path=='/containers/meridian-api-validation/json':return {'Image':'validated-image','Config':{'Labels':{'meridian.validation':'true'}}}
            if path.startswith('/images/'):return {'Id':'validated-image'}
            if method=='GET' and '-rollback-' in path:raise deployment.DockerFailure(404)
            return old if method=='GET' else None
        with patch.object(deployment,'engine',engine),patch.object(deployment,'verify'),patch.object(deployment,'prepare_compose',return_value=(lambda:None,lambda:None)),patch('sys.argv',['deploy.py','activate']):deployment.main()
        self.assertIn(('POST','/containers/old-id/update',{'RestartPolicy':{'Name':'no'}}),requests)
        created=next(body for _,path,body in requests if '/create?' in path)
        self.assertEqual({'Name':'always'},created['HostConfig']['RestartPolicy'])
    def test_multifile_compose_is_rejected_before_stop(self):
        old=self.original();old['Config']['Labels']['com.docker.compose.project.config_files']='base.yaml,override.yaml';requests=[]
        def engine(method,path,body=None):
            requests.append((method,path))
            if path=='/containers/meridian-api-validation/json':return {'Image':'validated-image','Config':{'Labels':{'meridian.validation':'true'}}}
            if path.startswith('/images/'):return {'Id':'validated-image'}
            return old
        with patch.object(deployment,'engine',engine),patch.object(deployment,'verify'),patch('sys.argv',['deploy.py','activate','--compose-file','base.yaml']):
            with self.assertRaisesRegex(RuntimeError,'Multi-file'):deployment.main()
        self.assertFalse(any('/stop' in path for _,path in requests))
    def test_different_image_is_rejected_before_stop(self):
        requests=[]
        def engine(method,path,body=None):
            requests.append((method,path))
            if path=='/containers/meridian-api-validation/json':return {'Image':'image-a','Config':{'Labels':{'meridian.validation':'true'}}}
            if path.startswith('/images/'):return {'Id':'image-b'}
            return self.original()
        with patch.object(deployment,'engine',engine),patch.object(deployment,'verify'),patch.object(deployment,'prepare_compose',return_value=(lambda:None,lambda:None)),patch('sys.argv',['deploy.py','activate']):
            with self.assertRaisesRegex(RuntimeError,'immutable image'):deployment.main()
        self.assertFalse(any('/stop' in path for _,path in requests))

    def test_compose_change_only_updates_the_selected_image_and_can_restore(self):
        import tempfile
        with tempfile.TemporaryDirectory() as directory:
            path=pathlib.Path(directory)/'compose.yaml'
            original='services:\n  database:\n    image: database:old\n  teslamateapi:\n    image: api:old\n    environment:\n      EXAMPLE: synthetic\n  grafana:\n    image: grafana:old\n'
            path.write_text(original)
            apply,restore=deployment.prepare_compose(str(path),'teslamateapi','api:new')
            apply();self.assertEqual(original.replace('image: api:old','image: api:new'),path.read_text())
            restore();self.assertEqual(original,path.read_text())

if __name__=='__main__':unittest.main()
