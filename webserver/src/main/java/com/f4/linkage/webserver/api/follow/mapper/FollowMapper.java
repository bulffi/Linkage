package com.f4.linkage.webserver.api.follow.mapper;

import com.f4.linkage.webserver.api.follow.model.FollowRelationship;
import com.f4.linkage.webserver.api.follow.model.FollowerInfo;
import com.f4.linkage.webserver.api.globalUser.model.InitialGlobalUser;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface FollowMapper {
  void addFollowRelationship(FollowRelationship relationship);
  List<String> getAllFollowerName(String globalUserName);
  int getAllFollowerNumber(String globalUserName);
  List<InitialGlobalUser> getGlobalUserIFollow(String username);
  List<String> getGlobalUserNameIFollowed(String username);
  void deleteFollow(String username ,String globalUserName);
  List<FollowerInfo> getMyFollowers(String globalUserName);
}
