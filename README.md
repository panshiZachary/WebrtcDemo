"# WebrtcDemo"
# WebRTC服务器搭建

网页即时通信（Web Real-Time Communication），是一个支持[网页浏览器](https://baike.baidu.com/item/%E7%BD%91%E9%A1%B5%E6%B5%8F%E8%A7%88%E5%99%A8)进行实时语音对话或视频对话的API。

**房间服务器**

​	开源实现: https://github.com/webrtc/apprtc

​	房间服务器是用来创建和管理通话会话的状态维护,是通话还是多方通话,加入与离开房间等等

**信令服务器**

​	apprtc中。

​	信令就是协调通讯的过程，为了建立一个webRTC的通讯过程，客户端需要交换如下信息

```
1. 会话控制信息，用来开始和结束通话，即开始视频、结束视频这些操作指令。
2. 发生错误时用来相互通告的消息
3. 元数据，如各自的音视频解码方式、带宽。
4. 网络数据，对方的公网IP、端口、内网IP及端口。
```

**内网穿透服务器**

https://github.com/coturn/coturn/wiki/Downloads

​	元数据是通过信令服务器中转发给另一个客户端,但是对于流媒体数据,一旦会话建立,首先尝试使用点对点连接。每个客户端都有一个唯一的地址,他能用来和其他客户端进行通讯和数据交换。

​	一般情况下,连接互联网时都处于防火墙后面或者配置私有子网的家庭路由器后面,导致我们的计算机的IP地址不是广域网IP地址,故而不能相互之间直接通讯。让两个同处于私有网络里的计算机能够通讯起来，这种技术通常称为NAT穿透。WebRTC 可以使用ICE框架去克服真实世界的复杂网络。

​	STUN (Simple Traversal of UDP Through NAT)，是一个完整的NAT穿透解决方案，即简单的用UDP穿透NAT。  

​	TURN (Traversal Using Relay NAT), 与STUN一样为了完成穿透效果，但是TURN是通过转发的方式来实现穿透。

​	ICE (Interactive Connectivity Establishment), 综合以上2种协议的综合性NAT穿越解决方案。首先会尝试用设备系统或网卡获取到的主机地址去建立连接；如果这个失败了（设备在NATs后面就会）ICE从STUN服务器获得外部的地址，如果这个也失败了，就用TURN中转服务器做通讯。 	





> 在阿里云后台开放端口(入队规则)端口: 3478、8080、8089、80、443

目录于: `/root/webrtc`

> Linux 后台运行的命令用：前面加 nohup 后面加 &



## 搭建AppRTC

安装需要的各种工具(除了apt之外还可以下载安装包或者源码自己编译安装)：

1、安装JDK

```shell
#为了演示初始环境 已重装系统, 重装后先执行:
apt-get update 

apt-get install openjdk-8-jdk 

java -version
#java version "1.8.0_181"
#Java(TM) SE Runtime Environment (build 1.8.0_181-b13)
#Java HotSpot(TM) 64-Bit Server VM (build 25.181-b13, mixed mode)
```

2、安装node.js

```shell
apt-get install nodejs-legacy 
apt-get install npm 
node --version
#v4.2.6
npm --version
#3.5.2

npm -g install grunt-cli
grunt --version
#grunt-cli v1.3.2
```

3、安装Python和Python-webtest （python2.7）

```shell
apt-get install python 
apt-get install python-webtest

python -V
#Python 2.7.12
```

4、安装google_appengine

```shell
wget https://storage.googleapis.com/appengine-sdks/featured/google_appengine_1.9.40.zip
unzip google_appengine_1.9.40.zip

#配置环境变量：在/etc/profile文件最后增加一行：
export PATH=$PATH:/root/webrtc/google_appengine
source /etc/profile
```

5、安装go

```shell
apt install golang-go

go version 
#go version go1.6.2 linux/amd64
```

```shell
#创建go工作目录
mkdir -p /root/webrtc/goWorkspace/src
#配置环境变量：在/etc/profile文件最后增加一行：
export GOPATH=/root/webrtc/goWorkspace
source /etc/profile
```

6、安装libevent

```shell
#当前目录:root/webrtc/
#https://github.com/coturn/coturn/wiki/CoturnConfig
wget https://github.com/downloads/libevent/libevent/libevent-2.0.21-stable.tar.gz
tar xvf libevent-2.0.21-stable.tar.gz
cd libevent-2.0.21-stable
./configure
make install
```

7、安装apprtc

```shell
#当前目录:root/webrtc/
git clone https://github.com/webrtc/apprtc.git
#将collider的源码软连接到go的工作目录下
ln -s /root/webrtc/apprtc/src/collider/collider $GOPATH/src
ln -s /root/webrtc/apprtc/src/collider/collidermain $GOPATH/src
ln -s /root/webrtc/apprtc/src/collider/collidertest $GOPATH/src
#编译collidermain
go get collidermain
go install collidermain

#go get collidermain: 被墙
#报错: package golang.org/x/net/websocket: unrecognized import path "golang.org/x/net/websocket"
#执行: 
#mkdir -p $GOPATH/src/golang.org/x/
#cd $GOPATH/src/golang.org/x/
#git clone https://github.com/golang/net.git net 
#go install net

```

8、安装coturn

```shell
#目录:root/webrtc/
#https://github.com/coturn/coturn/wiki/Downloads
wget http://coturn.net/turnserver/v4.5.0.7/turnserver-4.5.0.7.tar.gz
tar xvfz turnserver-4.5.0.7.tar.gz
cd turnserver-4.5.0.7
./configure
make install
```

 

配置与运行：

1、coturn Nat穿透服务器

配置防火墙，允许访问3478端口（含tcp和udp，此端口用于nat穿透）

```shell
#启动 --.--.--.--:内网ip(阿里云后台可以看到内外网ip)
nohup turnserver -L --.--.--.-- -a -u zachary:12345 -v -f -r nort.gov &
#账号zachary 密码:12345 这一步随便给，但是后面配置apprtc时需要用到
#命令后加 & ,执行起来后按 ctr+c,不会停止
```

```shell
#开启新窗口 执行
netstat -ntulp | grep turnserver #或者 lsof -i:3478
#输出大致这样的成功
tcp        0      0 127.0.0.1:5766          0.0.0.0:*                 LISTEN      16848/turnserver
tcp        0      0 172.31.247.136:3478       0.0.0.0:*               LISTEN      16848/turnserver
tcp        0      0 172.31.247.136:3478       0.0.0.0:*               LISTEN      16848/turnserver
udp        0      0 172.31.247.136:3478       0.0.0.0:*                           16848/turnserver
udp        0      0 172.31.247.136:3478       0.0.0.0:*                           16848/turnserver
```

2、collider 信令服务器

配置防火墙，允许访问8089端口（tcp，用于客户端和collider建立websocket信令通信）

```shell
#创建自签名的数字证书
#如果没有openssl,需要安装
mkdir -p /cert
cd /cert
# CA私钥
openssl genrsa -out key.pem 2048 
# 自签名证书
#启动 --.--.--.--:内网ip(阿里云后台可以看到内外网ip)
openssl req -new -x509 -key key.pem -out cert.pem -days 1095
nohup $GOPATH/bin/collidermain -port=8089 -tls=true  -room-server="https://--.--.--.--:8080" &
```

```shell
#同样检查是否成功
netstat -ntulp | grep collider
tcp6       0      0 :::8089                 :::*                    LISTEN      16864/collidermain
```

3、apprtc 房间服务器

配置防火墙，允许访问8080端口（tcp，此端口用于web访问）

配置文件修改（主要是配置apprtc对应的conturn和collider相关参数）

```shell
 vim /root/webrtc/apprtc/src/app_engine/constants.py

```

![修改后](修改后.png)

```shell
#编译
cd /root/webrtc/apprtc
npm install
grunt build
#如果出现 : No module named requests
```

> **错误: requests模块不存在**
>
> ```shell
> ImportError: No module named requests
> Warning: Command failed: python ./build/build_app_engine_package.py src out/app_engine
> Traceback (most recent call last):
>   File "./build/build_app_engine_package.py", line 12, in <module>
>     import requests
> ImportError: No module named requests
>  Use --force to continue.
> 
> Aborted due to warnings.
> ```
>
> ##### 安装pip
>
> 下载setup-python工具
>
> ```shell
> # 有一行命令太长了,$开头的是一行命令
> $cd /root/webrtc
> $wget https://pypi.python.org/packages/2.7/s/setuptools/setuptools-0.6c11-py2.7.egg  --no-check-certificate
> $chmod +x setuptools-0.6c11-py2.7.egg
> $./setuptools-0.6c11-py2.7.egg
> $wget https://pypi.python.org/packages/source/p/pip/pip-1.5.4.tar.gz 
> $tar -xf pip-1.5.4.tar.gz
> $python setup.py install
> $pip install requests
> #安装完成后再执行编译:
> #cd /root/webrtc/apprtc
> #grunt build
> ```



启动:

```shell
#--.--.--.-- : 内网ip
nohup dev_appserver.py --host=172.31.247.136 /root/webrtc/apprtc/out/app_engine --skip_sdk_update_check &
#提示更新选择: n
```

```shell
#检查
netstat -ntulp | grep 8080
#输出下列内容
tcp        0      0 172.31.18.14:8080       0.0.0.0:*               LISTEN      17032/python
```



4、nginx

反向代理apprtc，使之支持https访问，如果http直接访问apprtc，则客户端无法启动视频音频采集（必须得用https访问）

```shell
#在nginx目录执行  PCRE:apt-get install libpcre3-dev
./configure --with-http_ssl_module
make install
#默认安装在/usr/local/nginx（也可以执行prefix）
#配置nginx.conf
vim /usr/local/nginx/conf/nginx.conf
#内容如下(注意修改自己的公网ip)
```

```nginx
events {
 	worker_connections 1024;
}
http{
	upstream roomserver {
        server --.--.--.--:8080;
	}
	server {
		listen 80;
		server_name --.--.--.--;  
		return  301 https://$server_name$request_uri;
	}
	server {
		root /usr/share/nginx/html;
		index index.php index.html index.htm;
		listen      443 ssl;
		ssl_certificate /cert/cert.pem;
		ssl_certificate_key /cert/key.pem;
		server_name --.--.--.--;
		location / {
			proxy_pass http://roomserver$request_uri;
			proxy_set_header Host $host;
		}
		location ~ .php$ {
			fastcgi_pass unix:/var/run/php5-fpm.sock;
			fastcgi_index index.php;
			include fastcgi_params;
		}
	}
}
```

> 启动:
>
> /usr/local/nginx/sbin/nginx



> 浏览器通话跨域问题 :pushState
>
> Messages:Failed to start signaling: Failed to execute 'pushState' on 'History'
>
> ```shell
> vim /root/webrtc/apprtc/out/app_engine/js/apprtc.debug.js
> #搜索  pushState 增加:
> roomLink=roomLink.substring("http","https");
> ```
>
> ![跨域](跨域.png)













