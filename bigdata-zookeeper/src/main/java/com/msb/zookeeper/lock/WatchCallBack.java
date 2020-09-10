package com.msb.zookeeper.lock;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @author: 马士兵教育
 * @create: 2019-09-20 21:26
 */
public class WatchCallBack   implements Watcher, AsyncCallback.StringCallback ,AsyncCallback.Children2Callback ,AsyncCallback.StatCallback {

    ZooKeeper zk ;
    String threadName;
    CountDownLatch cc = new CountDownLatch(1);
    String pathName;

    public String getPathName() {
        return pathName;
    }

    public void setPathName(String pathName) {
        this.pathName = pathName;
    }

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public ZooKeeper getZk() {
        return zk;
    }

    public void setZk(ZooKeeper zk) {
        this.zk = zk;
    }

    public void tryLock(){
        try {

            System.out.println(threadName + "  create....");
//            if(zk.getData("/"))
            zk.create("/lock",threadName.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL,this,"abc");

            cc.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void unLock(){
        try {
            zk.delete(pathName,-1);
            System.out.println(threadName + " over work....");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (KeeperException e) {
            e.printStackTrace();
        }
    }


    //watch  -> 关注节点删除事件
    @Override
    public void process(WatchedEvent event) {


        //如果第一个哥们，那个锁释放了，其实只有第二个收到了回调事件！！
        //如果，不是第一个哥们，某一个，挂了，也能造成他后边的收到这个通知，从而让他后边那个跟去watch挂掉这个哥们前边的。。。
        switch (event.getType()) {
            case None:
                break;
            case NodeCreated:
                break;
            case NodeDeleted:
                //关心的时候前面一个节点，所以不需要watch
                //callback -> children2callback
                //最烧脑的部分
                zk.getChildren("/",false,this ,"sdf");
                break;
            case NodeDataChanged:
                break;
            case NodeChildrenChanged:
                break;
        }

    }

    //创建节点的callback
    @Override
    public void processResult(int rc, String path, Object ctx, String name) {
        if(name != null ){
            System.out.println(threadName  +"  create node : " +  name );
            pathName =  name ;
            //path：/testLock  ,不需要监控父目录,这样成本太大，callback=Children2Callback
            zk.getChildren("/",false,this ,"sdf");
        }

    }

    //getChildren  call back
    @Override
    public void processResult(int rc, String path, Object ctx, List<String> children, Stat stat) {

        //一定是：自己创建完了，并且一定能看到自己前边的创建的节点
        //取到的子节点，是乱序的，需要排序

//        System.out.println(threadName+"look locks.....");
//        for (String child : children) {
//            System.out.println(child);
//        }

        Collections.sort(children);
        int i = children.indexOf(pathName.substring(1));  //节点名称有/,需要截取


        //是不是第一个
        if(i == 0){
            //yes
            System.out.println(threadName +" i am first....");
            try {
                //没有业务处理，执行速度太快；导致后一个节点刚watch到第一个节点，但是第一个节点已经delete,导致后一个节点没有watch到delete事件
                //重入锁
                zk.setData("/",threadName.getBytes(),-1);
                cc.countDown();

            } catch (KeeperException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }else{
            //no
            //watcher: 节点状态变化，节点删除
            //callback: 还是该方法，是不是第一个节点
            zk.exists("/"+children.get(i-1),this,this,"sdf");
        }

    }


    @Override
    public void processResult(int rc, String path, Object ctx, Stat stat) {
        //偷懒
    }
}
