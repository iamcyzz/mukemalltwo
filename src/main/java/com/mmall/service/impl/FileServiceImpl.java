package com.mmall.service.impl;

import com.google.common.collect.Lists;
import com.mmall.service.IFileService;
import com.mmall.util.FTPUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Created by geely
 */
@Service("iFileService")
public class FileServiceImpl implements IFileService {

    private Logger logger = LoggerFactory.getLogger(FileServiceImpl.class);

    //上传的文件file，上传的路径path
    public String upload(MultipartFile file,String path){
        //拿到文件全名，包括扩展名
        String fileName = file.getOriginalFilename();
        //扩展名
        //abc.jpg
        //拿到.之后的扩展名，如 jpg,所以不要. 可以+1 直接就能不取.了
        String fileExtensionName = fileName.substring(fileName.lastIndexOf(".")+1);
        //重新拼接形成一个唯一的uuidname+扩展名
        String uploadFileName = UUID.randomUUID().toString()+"."+fileExtensionName;
        logger.info("开始上传文件,上传文件的文件名:{},上传的路径:{},新文件名:{}",fileName,path,uploadFileName);


        //目录的文件夹为fileDir
        File fileDir = new File(path);
        //判断这个文件夹是不是存在,不存在的话自动生成文件夹
        if(!fileDir.exists()){
            //设置可以自动创建文件夹的权限,才可以在tomcat下的webapp文件夹下创建文件夹的权限
            fileDir.setWritable(true);
            //自动创建一个文件夹
            fileDir.mkdirs();
        }//这样，上传文件目录就创建好了

        //然后创建文件，参数是放文件的路径，和文件名,这样就是一个完整的文件路径了
        //path:上传的路径, uploadFileName:上传文件的文件名
        File targetFile = new File(path,uploadFileName);


        try {
            //调用这个spring封装的方法，文件就能上传成功了
            //这样文件就能传到对应的文件夹的下面了，这样我们是让文件传到upload的这个文件夹下
            file.transferTo(targetFile);

            //然后再把这个文件传到ftp上面去
            FTPUtil.uploadFile(Lists.newArrayList(targetFile));

            //已经上传到ftp服务器上，就可以删除存到本地的图片了
            targetFile.delete();

        } catch (IOException e) {
            logger.error("上传文件异常",e);
            return null;
        }
        //A:abc.jpg
        //B:abc.jpg
        return targetFile.getName();
    }

}
