package com.f4.linkage.fileserver.dao;

import com.f4.linkage.fileserver.mapper.CommentMapper;
import com.f4.linkage.fileserver.mapper.LikeMapper;
import com.f4.linkage.fileserver.mapper.MomentMapper;
import com.f4.linkage.fileserver.model.Moment;
import com.f4.linkage.fileserver.model.Comment;
import com.f4.linkage.fileserver.model.Like;
import com.f4.linkage.fileserver.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Comparator;
import java.util.List;

@Service
public class MomentDao {
    private final static Logger LOGGER = LoggerFactory.getLogger(MomentDao.class);

    @Value("${app.linkage.fileRoot}")
    private String fileRoot;

    @Resource
    JdbcTemplate jdbcTemplate;

    public boolean insertMoment(Object[] args){
        String sql = "INSERT INTO moment(poster_name, text, picture_num, video_num) VALUES (?,?,?,?)";
        if(jdbcTemplate.update(sql, args) == 1) {
            String poster = args[0].toString();
            List<String> friends = getFriends(poster);
            for (String friend: friends
                 ) {
                insertWSSM(friend, FileUtil.momentID);
            }
            return true;
        }else {
            return false;
        }
    }

    public boolean updateIconUrl(String username){
        String iconUrl = "/icon/" + username;
        String sql = "Update user set iconUrl = ? where username = ?";
        if(jdbcTemplate.update(sql, new Object[]{iconUrl, username}) == 1){
            return true;
        }else {
            return false;
        }
    }

    public boolean updateGlobalIconUrl(String username){
        String iconUrl = "/global_icon/" + username;
        String sql = "Update global_user set iconUrl = ? where username = ?";
        if(jdbcTemplate.update(sql, new Object[]{iconUrl, username}) == 1){
            return true;
        }else {
            return false;
        }
    }

    public List<String> getFriends(String username){
        String sql = "select friend_username from friend where my_username=?";
        List<String> friends = jdbcTemplate.queryForList(sql, new Object[]{username}, String.class);
        friends.add(username);
        return friends;
    }

    public void insertWSSM(String username, int moment_id){
        String sql = "insert into who_should_see_moment values(null, ?, ?)";
        jdbcTemplate.update(sql, new Object[]{moment_id, username});
    }

    public boolean deleteMoment(int id){
        String sql = "delete from moment where id=?";
        if(jdbcTemplate.update(sql, new Object[]{id}) == 1){
            return true;
        }
        else {
            return false;
        }
    }

    public List<Moment> getMoments(String username){
        String moment_sql = "select * from moment where id in " +
                "(select moment_id from who_should_see_moment where username=?) order by id desc ";
        Object[] arg = new Object[]{username};
        List<Moment> momentList = jdbcTemplate.query(moment_sql, new Object[]{username}, new MomentMapper());

        for (Moment moment:momentList
             ) {
            int momentId = moment.getId();
            moment.setSelf_like(false);
            List<Like> likeList = getMomentLikeList(momentId);
            moment.setLike(likeList);
            moment.setComment(getMomentCommentList(momentId));
            moment.setSelf_like(amILiker(likeList, username));
        }
        return momentList;
    }

    public List<Moment> getPrivateMoments(String username){
        String sql = "select * from moment where poster_name=? order by id desc ";

        List<Moment> momentList = jdbcTemplate.query(sql, new Object[]{username}, new MomentMapper());
        for (Moment moment:momentList
             ) {
            int momentId = moment.getId();
            List<Like> likeList = getMomentLikeList(momentId);
            moment.setLike(likeList);
            moment.setComment(getMomentCommentList(momentId));
            moment.setSelf_like(amILiker(likeList, username));
        }
        return momentList;
    }

    public List<Like> getMomentLikeList(int momentId){
        String like_sql = "select * from moment_like where liked_id=?";
        return jdbcTemplate.query(like_sql, new Object[]{momentId}, new LikeMapper());
    }

    public List<Comment> getMomentCommentList(int momentId){
        String comment_sql = "select * from moment_comment where commented_id=?";
        return jdbcTemplate.query(comment_sql, new Object[]{momentId}, new CommentMapper());
    }

    private boolean amILiker(List<Like> likeList, String username){
        for (Like momentLike:likeList
             ) {
            if(momentLike.getLiker().equals(username)){
                return true;
            }
        }
        return false;
    }

    public boolean updateMomentLike(String username, int momentId, boolean action) {
        String sql_query = "select count(*) from moment_like where liker=? and liked_id=?";
        String sql_insert = "insert into moment_like(liker, liked_id) value(?,?)";
        String sql_delete = "delete from moment_like where liker=? and liked_id=?";
        Object[] args = new Object[]{username, momentId};
        boolean exist = jdbcTemplate.queryForObject(sql_query, args, Integer.class).equals(1);
        if(action){
            if(exist || jdbcTemplate.update(sql_insert, args) == 1){
                return true;
            }else {
                return false;
            }
        }else {
            if(!exist || jdbcTemplate.update(sql_delete, args) == 1){
                return true;
            }else {
                return false;
            }
        }
    }

    public boolean insertComment(String username, int momentId, String comment){
        String sql = "insert into moment_comment(commenter, commented_id, content) value (?,?,?)";
        if(jdbcTemplate.update(sql, new Object[]{username, momentId, comment}) == 1){
            return true;
        }else {
            return false;
        }
    }

    public boolean deleteComment(String username, int commentId){
        String sql = "delete from moment_comment where id=?";

        if(jdbcTemplate.update(sql, new Object[]{commentId}) == 1){
            return true;
        }else {
            return false;
        }
    }

    public String getGlobalName(String localName){
        String sql = "select global_username from local_global_user where local_username=?";
        String globalName = null;
        try {
            globalName = jdbcTemplate.queryForObject(sql, new Object[]{localName}, String.class);
        }catch (Exception e){
            e.printStackTrace();
        }
        return globalName;
    }
}
