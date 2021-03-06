package com.f4.linkage.fileserver.util;

import com.f4.linkage.fileserver.model.FileKind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class FileUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtil.class);
    public static int momentID = -1;
    public static int postID = -1;
    public static AtomicInteger imgID = new AtomicInteger(0);

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Value("${app.linkage.fileRoot}")
    private String fileRoot;
    @Value("${app.linkage.defaultIcon}")
    private String defaultIcon;
    @Value("${app.linkage.defaultGlobalIcon}")
    private String defaultGlobalIcon;

    public boolean saveFiles(MultipartFile[] files, FileKind fileKind, List<String> serverSideUrls){
        try {
            for (int i = 0; i < files.length; i++) {
                MultipartFile file = files[i];
                file.transferTo(getServerFilePath(i, file, fileKind));
                if(serverSideUrls != null){
                    serverSideUrls.add("/post/picture/" + imgID.getAndAdd(1));
                }
            }
        }catch (IOException e){
            return false;
        }
        return true;
    }

    public boolean saveHtml(String content, String path){
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(path));
            writer.write(content);
            writer.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public boolean saveIconFile(MultipartFile file, String user){
        try{
            file.transferTo(Paths.get(fileRoot + "icon/"+ user));
        }catch (IOException e){
            return false;
        }
        return true;
    }

    public boolean saveGlobalIconFile(MultipartFile file, String user){
        try{
            file.transferTo(Paths.get(fileRoot + "global_icon/"+ user));
        }catch (IOException e){
            return false;
        }
        return true;
    }

    private Path getServerFilePath(int index, MultipartFile file, FileKind fileKind){
        String fileName = file.getOriginalFilename();
        LOGGER.info("file name is " + fileName);
        String prefix = "";
        switch (fileKind){
            case MomentPicture:
                prefix = "moment/img/" + momentID;
                break;
            case MomentVideo:
                prefix = "moment/video/" + momentID;
                break;
            case PostPicture:
                return  Paths.get(fileRoot + "post/img/" + imgID);
            case PostVideo:
                prefix = "post/video/" + postID;
                break;
        }

        return Paths.get(fileRoot + prefix + "_" + index);
    }

    private String getPosix(String name){
        if(name.isEmpty()){
            return "";
        }
        String[] splits = name.split("\\.");
        return splits[splits.length - 1];
    }

    public void updateMomentID(){
        if(momentID < 0){
            String query = "SELECT COUNT(*) FROM moment";
            momentID = jdbcTemplate.queryForObject(query, Integer.class) + 1;
        }
        else {
            momentID++;
        }
    }

    public void updatePostID(){
        if(postID < 0){
            String query = "SELECT COUNT(*) FROM post";
            postID = jdbcTemplate.queryForObject(query, Integer.class) + 1;
        }
        else {
            postID++;
        }
    }

    public void transfer(HttpServletResponse response, String path, FileKind fileKind) {
        File file = new File(fileRoot + path);
        if (!file.exists()) {
            if(fileKind.equals(FileKind.ICON)){
                file = new File(defaultIcon);
            }
            else if(fileKind.equals(FileKind.GLOBAL_ICON)){
                file = new File(defaultGlobalIcon);
            }
            else {
                response.setStatus(404);
                try{
                    response.getWriter().println("The file you request is not found!");
                }catch (IOException e){
                }
            }
        }
        try{
            transfer(response.getOutputStream(), file);
        }catch (IOException e){
            response.setStatus(500);
            return;
        }
    }

    private void transfer(OutputStream outputStream, File file) throws IOException{
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            byte[] content = new byte[fis.available()];
            fis.read(content);
            outputStream.write(content);
                /*int count = 0;
                byte[] buffer = new byte[1024 * 8];
                while ((count = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, count);
                    os.flush();
                }*/
        }catch (IOException e) {
            throw e;
        }finally {
            try {
                fis.close();
                outputStream.close();
            } catch (IOException e) {
                throw e;
            }
        }
    }
}
