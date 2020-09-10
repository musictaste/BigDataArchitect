package com.msb.zookeeper.config;

import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.util.concurrent.CountDownLatch;

/**
 * @author: 马士兵教育
 * @create: 2019-09-20 20:21
 */
public class WatchCallBack  implements Watcher ,AsyncCallback.StatCallback, AsyncCallback.DataCallback {

    ZooKeeper zk ;
    MyConf conf ;
    CountDownLatch cc = new CountDownLatch(1);

    public MyConf getConf() {
        return conf;
    }

    public void setConf(MyConf conf) {
        this.conf = conf;
    }

    public ZooKeeper getZk() {
        return zk;
    }

    public void setZk(ZooKeeper zk) {
        this.zk = zk;
    }

    //语义：无论节点存在不存在，取节点数据
    public void aWait(){
        //路径是：/testLock/AppConf
        zk.exists("/AppConf",this,this ,"ABC");
        try {
            cc.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //有数据的callback，getData触发的callback
    @Override
    public void processResult(int rc, String path, Object ctx, byte[] data, Stat stat) {

        if(data != null ){
            String s = new String(data);
            conf.setConf(s);
            cc.countDown();
        }


    }

    //状态的callback,exists触发的callback
    @Override
    public void processResult(int rc, String path, Object ctx, Stat stat) {
        //节点存在的话，get数据
        if(stat != null){
            //getData消耗一个watch以后，再注册了一个watch
            zk.getData("/AppConf",this,this,"sdfs");
        }

    }

    //对节点的watch
    @Override
    public void process(WatchedEvent event) {

        switch (event.getType()) {
            case None:
                break;
            case NodeCreated:
                zk.getData("/AppConf",this,this,"sdfs");

                break;
            case NodeDeleted:
                //容忍性：如果要求数据一致性就取配置，不要求数据一致性则不取配置
                conf.setConf("");
                cc = new CountDownLatch(1); //重新设置为1
                break;
            case NodeDataChanged:
                zk.getData("/AppConf",this,this,"sdfs");

                break;
            case NodeChildrenChanged:
                break;
        }

    }
}
