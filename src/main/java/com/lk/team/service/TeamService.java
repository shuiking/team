package com.lk.team.service;

import com.lk.team.model.entity.Team;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lk.team.model.entity.User;
import com.lk.team.model.request.*;
import com.lk.team.model.vo.TeamUserVo;
import com.lk.team.model.vo.TeamVo;
import com.lk.team.model.vo.UserVo;

import javax.servlet.http.HttpServletRequest;
import java.util.Set;

/**
* @author k
* @description 针对表【team(队伍)】的数据库操作Service
* @createDate 2023-05-05 19:22:29
*/
public interface TeamService extends IService<Team> {
    /**
     * 获取所有队伍
     * @return
     */
    TeamUserVo getTeams();


    /**
     * 根据队伍id获取队伍
     * @param teamId
     * @param loginUser
     * @return
     */
    TeamVo getUsersByTeamId(Long teamId, UserVo loginUser);

    /**
     * 根据用户加入的队伍id获取队伍信息
     * @param teamId
     * @param loginUser
     * @return
     */
    TeamUserVo getTeamListByTeamIds(Set<Long> teamId,UserVo loginUser);

    /**
     * 创建新的队伍
     * @param teamCreateRequest
     * @param loginUser
     * @return
     */
    boolean createTeam(TeamCreateRequest teamCreateRequest, UserVo loginUser);

    /**
     * 解散队伍
     * @param teamId
     * @param loginUser
     * @return
     */
    boolean dissolutionTeam(Long teamId, UserVo loginUser);

    /**
     * 退出队伍
     * @param teamId
     * @param loginUser
     * @return
     */
    boolean quitTeam(Long teamId, UserVo loginUser);

    /**
     * 修改队伍信息
     * @param teamUpdateRequest
     * @param loginUser
     * @return
     */
    Boolean updateTeam(TeamUpdateRequest teamUpdateRequest, UserVo loginUser);

    /**
     * 加入队伍
     * @param joinTeam
     * @param loginUser
     * @return
     */
    UserVo joinTeam(TeamJoinRequest joinTeam, UserVo loginUser);


    /**
     * 踢出队员
     * @param kickOutUserRequest
     * @param loginUser
     * @return
     */
    Boolean kickOutTeamByUserId(KickOutUserRequest kickOutUserRequest, UserVo loginUser);

    /**
     * 查询队伍
     * @param teamQueryRequest
     * @return
     */
    TeamUserVo teamQuery(TeamQueryRequest teamQueryRequest);

    /**
     * 转交队长
     * @param transferTeamRequest
     * @param loginUser
     * @return
     */
    Boolean transferTeam(TransferTeamRequest transferTeamRequest, UserVo loginUser);
}
