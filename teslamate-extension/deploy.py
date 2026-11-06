#!/usr/bin/env python3
"""Deploy a validated API image, reusing existing credentials only in memory."""
import argparse
import http.client
import json
import socket
import time
import urllib.request

class DockerFailure(RuntimeError):
    def __init__(self,status):self.status=status;super().__init__('Docker operation failed: '+str(status))

class Engine(http.client.HTTPConnection):
    def __init__(self):
        super().__init__('localhost', timeout=40)
    def connect(self):
        self.sock=socket.socket(socket.AF_UNIX,socket.SOCK_STREAM)
        self.sock.connect('/var/run/docker.sock')

def engine(method,path,body=None):
    conn=Engine()
    payload=None if body is None else json.dumps(body).encode()
    conn.request(method,path,payload,{'Content-Type':'application/json'})
    response=conn.getresponse();raw=response.read();status=response.status;conn.close()
    if status>=400:raise DockerFailure(status)
    return json.loads(raw) if raw else None

def verify(port,host="127.0.0.1"):
    import datetime,urllib.parse
    base='http://'+host+':'+str(port)+'/api/v1/'
    def read(path):
        with urllib.request.urlopen(base+path,timeout=20) as response:return json.load(response)['data']
    for _ in range(15):
        try:
            cars=read('cars')['cars'];assert cars
            car=cars[0]['car_id'];assert isinstance(car,int) and car>0
            prefix='cars/'+str(car)+'/meridian/'
            assert read(prefix+'features')['read_only'] is True
            now=datetime.datetime.now(datetime.timezone.utc)
            query=urllib.parse.urlencode({'startDate':(now-datetime.timedelta(days=7)).isoformat(),'endDate':now.isoformat(),'show':1})
            for kind,key in [('charges?show=1','records'),('drives?show=1','records'),('history?'+query,'samples'),('tracks?'+query,'points')]:
                data=read(prefix+kind);assert data['car_id']==car and isinstance(data[key],list)
            return
        except Exception:time.sleep(1)
    raise RuntimeError('Bounded API query verification failed')

def optional_container(name):
    try:return engine('GET','/containers/'+name+'/json')
    except DockerFailure as error:
        if error.status==404:return None
        raise

def prepare_compose(path,service,image):
    import os,re,tempfile,pathlib
    if not path or not pathlib.Path(path).is_file():raise RuntimeError('An existing Compose file is required for persistent activation')
    target=pathlib.Path(path);original=target.read_text();lines=original.splitlines(keepends=True)
    header=re.compile(r'^( *)'+re.escape(service)+r':\s*(?:#.*)?$')
    candidates=[i for i,line in enumerate(lines) if header.match(line.rstrip())]
    if len(candidates)!=1:raise RuntimeError('Compose service is ambiguous')
    begin=candidates[0];indent=len(lines[begin])-len(lines[begin].lstrip());finish=len(lines)
    for i in range(begin+1,len(lines)):
        if lines[i].strip() and not lines[i].lstrip().startswith('#') and len(lines[i])-len(lines[i].lstrip())<=indent:finish=i;break
    matches=[i for i in range(begin+1,finish) if re.match(r'^ {'+str(indent+2)+r'}image:',lines[i])]
    if len(matches)!=1:raise RuntimeError('Compose image declaration is ambiguous')
    index=matches[0];lines[index]=' '*(indent+2)+'image: '+image+'\n';updated=''.join(lines)
    def write(value):
        fd,tmp=tempfile.mkstemp(prefix='.meridian-compose-',dir=str(target.parent))
        try:
            with os.fdopen(fd,'w') as stream:stream.write(value);stream.flush();os.fsync(stream.fileno())
            os.chmod(tmp,target.stat().st_mode&0o777);os.replace(tmp,target)
        finally:
            if os.path.exists(tmp):os.unlink(tmp)
    def apply():
        if target.read_text()!=original:raise RuntimeError('Compose file changed during verification')
        write(updated)
    def restore():
        current=target.read_text()
        if current==updated:write(original)
        elif current!=original:raise RuntimeError('Compose file changed independently')
    return apply,restore

def main():
    p=argparse.ArgumentParser();p.add_argument('mode',choices=['validate','activate']);p.add_argument('--image',default='meridian-teslamateapi:1.25.0-meridian.1');p.add_argument('--validation-host',default='127.0.0.1');p.add_argument('--compose-file');args=p.parse_args()
    primary='teslamate-teslamateapi-1';old=engine('GET','/containers/'+primary+'/json')
    network=old['HostConfig']['NetworkMode']
    config={k:old['Config'][k] for k in ['Env','Cmd','Entrypoint','User','WorkingDir','ExposedPorts'] if k in old['Config']}
    config['Image']=args.image
    config['HostConfig']={k:old['HostConfig'][k] for k in ['NetworkMode','LogConfig','RestartPolicy'] if k in old['HostConfig']}
    if args.mode=='validate':
        config['Labels']={'meridian.validation':'true'}
        config['HostConfig']['RestartPolicy']={'Name':'no'}
        config['HostConfig']['PortBindings']={'8080/tcp':[{'HostIp':args.validation_host,'HostPort':'18081'}]}
        name='meridian-api-validation'
        existing=optional_container(name)
        if existing:
            if existing['Config'].get('Labels',{}).get('meridian.validation')!='true':raise RuntimeError('Validation name is occupied')
            engine('DELETE','/containers/'+name+'?force=true')
        config['NetworkingConfig']={'EndpointsConfig':{network:{'Aliases':[name]}}}
        engine('POST','/containers/create?name='+name,config);engine('POST','/containers/'+name+'/start');verify(18081,args.validation_host)
        print(json.dumps({'mode':'validation','healthy':True,'port':18081}))
        return
    validation=optional_container('meridian-api-validation')
    requested=engine('GET','/images/'+args.image+'/json')
    if not validation or validation['Config'].get('Labels',{}).get('meridian.validation')!='true' or validation['Image']!=requested['Id']:
        raise RuntimeError('Requested image is not the validated immutable image')
    verify(18081,args.validation_host)
    compose_files=old['Config'].get('Labels',{}).get('com.docker.compose.project.config_files','')
    if ',' in compose_files:raise RuntimeError('Multi-file Compose projects require a separately verified deployment')
    compose_path=args.compose_file or compose_files
    apply_compose,restore_compose=prepare_compose(compose_path,old['Config'].get('Labels',{}).get('com.docker.compose.service','teslamateapi'),args.image)
    config['Image']=validation['Image']
    config['Labels']=dict(old['Config'].get('Labels',{}))
    config['Labels']['com.docker.compose.project.config_files']=compose_path
    import os
    config['Labels']['com.docker.compose.project.working_dir']=os.path.dirname(compose_path)
    config['HostConfig']['PortBindings']=old['HostConfig']['PortBindings']
    aliases=old['NetworkSettings']['Networks'][network].get('Aliases',[])
    config['NetworkingConfig']={'EndpointsConfig':{network:{'Aliases':aliases}}}
    backup=primary+'-rollback-'+str(time.time_ns())
    if optional_container(backup):raise RuntimeError('Rollback name is occupied')
    try:
        engine('POST','/containers/'+old['Id']+'/stop?t=10')
        engine('POST','/containers/'+old['Id']+'/rename?name='+backup)
        engine('POST','/containers/create?name='+primary,config)
        engine('POST','/containers/'+primary+'/start');verify(8081);apply_compose()
        engine('POST','/containers/'+old['Id']+'/update',{'RestartPolicy':{'Name':'no'}})
    except BaseException as error:
        compose_restored=True
        try:restore_compose()
        except Exception:compose_restored=False
        current=optional_container(primary)
        if current and current['Id']!=old['Id']:
            if current['Image']!=validation['Image']:raise RuntimeError('Unexpected primary identity; restore requires inspection') from None
            engine('DELETE','/containers/'+current['Id']+'?force=true')
        original=engine('GET','/containers/'+old['Id']+'/json')
        if original['Name']!='/'+primary:engine('POST','/containers/'+old['Id']+'/rename?name='+primary)
        engine('POST','/containers/'+old['Id']+'/update',{'RestartPolicy':old['HostConfig']['RestartPolicy']})
        engine('POST','/containers/'+old['Id']+'/start')
        if not isinstance(error,Exception):raise
        raise RuntimeError('Activation failed; previous API restored'+('' if compose_restored else '; Compose restore requires inspection')) from None
    print(json.dumps({'mode':'activation','healthy':True,'rollback_container':backup,'previous_image':old['Image'],'active_image':validation['Image']}))

if __name__=='__main__':main()
