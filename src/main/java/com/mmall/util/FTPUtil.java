package com.mmall.util;

import org.apache.commons.net.ftp.FTPClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

/**
 * Created by geely
 */
public class FTPUtil {

    private static  final Logger logger = LoggerFactory.getLogger(FTPUtil.class);

    private static String ftpIp = PropertiesUtil.getProperty("ftp.server.ip");
    private static String ftpUser = PropertiesUtil.getProperty("ftp.user");
    private static String ftpPass = PropertiesUtil.getProperty("ftp.pass");


    private String ip;
    private int port;
    private String user;
    private String pwd;
    private FTPClient ftpClient;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    public FTPClient getFtpClient() {
        return ftpClient;
    }

    public void setFtpClient(FTPClient ftpClient) {
        this.ftpClient = ftpClient;
    }



    public FTPUtil(String ip,int port,String user,String pwd){
        this.ip = ip;
        this.port = port;
        this.user = user;
        this.pwd = pwd;
    }
    public static boolean uploadFile(List<File> fileList) throws IOException {
        FTPUtil ftpUtil = new FTPUtil(ftpIp,21,ftpUser,ftpPass);
        logger.info("开始连接ftp服务器");
        boolean result = ftpUtil.uploadFile("img",fileList);
        logger.info("开始连接ftp服务器,结束上传,上传结果:{}");
        return result;
    }

    //remotePath就是我们要上传到ftp里面的文件夹路径,ftp在linux里面相当一个文件夹,图片就是传到这个远程的文件夹，所以就需要用remotePath这个参数
    private boolean uploadFile(String remotePath,List<File> fileList) throws IOException {
        boolean uploaded = true;
        FileInputStream fis = null;
        //连接FTP服务器,封装这几个方法
        if(connectServer(this.ip,this.port,this.user,this.pwd)){
            try {
                //登陆成功后先检查要放的文件夹，看看需要不需要切换要放的文件夹
                //假如remotePath为空值，就切换不了了
                ftpClient.changeWorkingDirectory(remotePath);
                //设置缓冲区
                ftpClient.setBufferSize(1024);
                //防止中文乱码
                ftpClient.setControlEncoding("UTF-8");
                //设置为二进制
                ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
                //打开本地的被动模式
                ftpClient.enterLocalPassiveMode();
                for(File fileItem : fileList){
                    //然后通过inputStream,把文件放入流
                    fis = new FileInputStream(fileItem);
                    //通过这个储存方法，然后文件名是什么，然后把流fis给它放进来
                    ftpClient.storeFile(fileItem.getName(),fis);
                }

            } catch (IOException e) {
                logger.error("上传文件异常",e);
                //假如异常这样我们设置为false，我们就能看到了
                uploaded = false;
                e.printStackTrace();
            } finally {
                //上传成功了要释放资源
                //关闭流
                fis.close();
                //关闭连接资源
                ftpClient.disconnect();
            }
        }
        return uploaded;
    }


    //封装链接ftp服务器的方法
    private boolean connectServer(String ip,int port,String user,String pwd){

        boolean isSuccess = false;
        ftpClient = new FTPClient();
        try {
            ftpClient.connect(ip);//连接的远程ip

            isSuccess = ftpClient.login(user,pwd);//输入账户密码,这个login方法也是返回一个布尔，判断登陆成功还是失败
        } catch (IOException e) {
            logger.error("连接FTP服务器异常",e);
        }
        return isSuccess;
    }












}
