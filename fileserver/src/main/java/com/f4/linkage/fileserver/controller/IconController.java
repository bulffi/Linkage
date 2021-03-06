package com.f4.linkage.fileserver.controller;

import com.f4.linkage.fileserver.dao.MomentDao;
import com.f4.linkage.fileserver.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.security.Principal;

@RestController
public class IconController {
    private final static Logger LOGGER = LoggerFactory.getLogger(IconController.class);

    @Resource
    private FileUtil fileUtil;

    @Resource
    private MomentDao momentDao;



    @PostMapping("/icon")
    ResponseEntity<String> uploadIcon(Principal principal, @RequestParam("Icon") MultipartFile icon){
        String username = principal.getName();
        if(icon != null){
            if(fileUtil.saveIconFile(icon, username)){
                momentDao.updateIconUrl(username);
                return ResponseEntity.ok().body("/icon/" + username);
            }
            else {
                return ResponseEntity.status(500).body("We can't save your icon file!");
            }
        }
        return ResponseEntity.status(400).body("We don't receive your icon file");
    }

    @PostMapping("/global_icon")
    ResponseEntity<String> uploadGlobalIcon(Principal principal, @RequestParam("Icon") MultipartFile icon){
        String localName = principal.getName();
        String globalName = momentDao.getGlobalName(localName);
        if(globalName == null){
            return ResponseEntity.status(400).body("You don't have a global account!");
        }
        if(icon != null){
            if(fileUtil.saveGlobalIconFile(icon, globalName)){
                momentDao.updateGlobalIconUrl(globalName);
                return ResponseEntity.ok().body("/global_icon/" + globalName);
            }
            else {
                return ResponseEntity.status(500).body("We can't save your icon file!");
            }
        }
        return ResponseEntity.status(400).body("We don't receive your icon file");

    }
}
