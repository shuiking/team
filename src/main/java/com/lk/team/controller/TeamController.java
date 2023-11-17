package com.lk.team.controller;

import com.lk.team.common.BaseResponse;
import com.lk.team.common.ErrorCode;
import com.lk.team.common.ResultUtil;
import com.lk.team.constant.RedisConstant;
import com.lk.team.constant.UserConstant;
import com.lk.team.exception.ApiException;
import com.lk.team.model.request.*;
import com.lk.team.model.vo.TeamUserVo;
import com.lk.team.model.vo.TeamVo;
import com.lk.team.model.vo.UserVo;
import com.lk.team.service.TeamService;
import com.lk.team.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Set;

/**
 * @Author : lk
 * @create 2023/5/5 18:31
 */
@Api(tags = "组队管理")
@RestController
@RequestMapping("/team")
@Slf4j
public class TeamController {
    @Autowired
    private TeamService teamService;
    @Autowired
    private UserService userService;
    @Autowired
    private RedisTemplate<String,Object> redisTemplate;

    @GetMapping("/teams")
    @ApiOperation("获取队伍列表")
    public BaseResponse<TeamUserVo> getTeams() {
        TeamUserVo teams = teamService.getTeams();
        return ResultUtil.success(teams);
    }

    @GetMapping("/{teamId}")
    @ApiOperation("根据队伍id获取队伍信息")
    public BaseResponse<TeamVo> getUsersByTeamId(@PathVariable("teamId") Long teamId, HttpServletRequest request) {
        if (teamId == null || teamId <= 0) {
            throw new ApiException(ErrorCode.PARAMS_ERROR);
        }
        UserVo loginUser = userService.getLoginUser(request);
        TeamVo teams = teamService.getUsersByTeamId(teamId,loginUser);
        return ResultUtil.success(teams);
    }

    @GetMapping("/teamsByIds")
    @ApiOperation("获取用户加入的队伍列表")
    public BaseResponse<TeamUserVo> getTeamListByTeamIds(@RequestParam(required = false) Set<Long> teamId, HttpServletRequest request) {
        if (CollectionUtils.isEmpty(teamId)) {
            throw new ApiException(ErrorCode.PARAMS_ERROR);
        }
        UserVo loginUser = userService.getLoginUser(request);
        TeamUserVo teams = teamService.getTeamListByTeamIds(teamId,loginUser);
        return ResultUtil.success(teams);
    }

    @PostMapping("/createTeam")
    @ApiOperation("创建队伍")
    public BaseResponse<Boolean> createTeam(@RequestBody TeamCreateRequest teamCreateRequest,HttpServletRequest request) {
        if (teamCreateRequest == null) {
            throw new ApiException(ErrorCode.NULL_ERROR, "创建队伍失败");
        }
        UserVo loginUser = userService.getLoginUser(request);

        Boolean team = teamService.createTeam(teamCreateRequest, loginUser);
        //删除缓存
        redisTemplate.delete(RedisConstant.TEAM_LIST);
        redisTemplate.delete(RedisConstant.TEAM_USER);
        return ResultUtil.success(team);
    }

    @PostMapping("/{teamId}")
    @ApiOperation("解散队伍")
    public BaseResponse<Boolean> dissolutionByTeamId(@PathVariable("teamId") Long teamId, HttpServletRequest request) {
        if (teamId == null || teamId <= 0) {
            throw new ApiException(ErrorCode.PARAMS_ERROR, "该用户暂未加入队伍");
        }
        UserVo loginUser = userService.getLoginUser(request);
        boolean dissolutionTeam = teamService.dissolutionTeam(teamId, loginUser);
        //删除缓存
        redisTemplate.delete(RedisConstant.TEAM_LIST);
        redisTemplate.delete(RedisConstant.TEAM_USER);
        return ResultUtil.success(dissolutionTeam);
    }

    @PostMapping("/quit/{teamId}")
    @ApiOperation("退出队伍")
    public BaseResponse<Boolean> quitTeam(@PathVariable("teamId") Long teamId, HttpServletRequest request) {
        if (teamId == null || teamId <= 0) {
            throw new ApiException(ErrorCode.PARAMS_ERROR, "该用户暂未加入队伍");
        }
        UserVo loginUser = userService.getLoginUser(request);
        boolean quitTeam = teamService.quitTeam(teamId, loginUser);
        //删除缓存
        redisTemplate.delete(RedisConstant.TEAM_LIST);
        redisTemplate.delete(RedisConstant.TEAM_USER);
        return ResultUtil.success(quitTeam);
    }

    @PostMapping("/update")
    @ApiOperation("更新队伍")
    public BaseResponse<Boolean> updateTeam(@RequestBody TeamUpdateRequest teamUpdateRequest,HttpServletRequest request) {
        if (teamUpdateRequest == null) {
            throw new ApiException(ErrorCode.NULL_ERROR, "修改队伍失败");
        }
        UserVo loginUser = userService.getLoginUser(request);
        Boolean team = teamService.updateTeam(teamUpdateRequest, loginUser);
        //删除缓存
        redisTemplate.delete(RedisConstant.TEAM_LIST);
        redisTemplate.delete(RedisConstant.TEAM_USER);
        return ResultUtil.success(team);
    }

    @PostMapping("/join")
    @ApiOperation("加入队伍")
    public BaseResponse<UserVo> joinTeam(@RequestBody TeamJoinRequest joinTeam,HttpServletRequest request) {
        if (joinTeam == null) {
            throw new ApiException(ErrorCode.NULL_ERROR, "加入队伍失败");
        }
        UserVo loginUser = userService.getLoginUser(request);
        UserVo joinUser = teamService.joinTeam(joinTeam, loginUser);
        //删除缓存
        redisTemplate.delete(RedisConstant.TEAM_LIST);
        redisTemplate.delete(RedisConstant.TEAM_USER);
        return ResultUtil.success(joinUser);
    }

    @PostMapping("/kickOutUser")
    @ApiOperation("把用户踢出队伍")
    public BaseResponse<Boolean> kickOutTeamByUserId(@RequestBody KickOutUserRequest kickOutUserRequest,HttpServletRequest request) {
        if (kickOutUserRequest == null) {
            throw new ApiException(ErrorCode.NULL_ERROR, "该用户不存在");
        }
        UserVo loginUser = userService.getLoginUser(request);
        Boolean kickOut = teamService.kickOutTeamByUserId(kickOutUserRequest, loginUser);
        //删除缓存
        redisTemplate.delete(RedisConstant.TEAM_LIST);
        redisTemplate.delete(RedisConstant.TEAM_USER);
        return ResultUtil.success(kickOut);
    }

    @PostMapping("/search")
    @ApiOperation("查询队伍")
    public BaseResponse<TeamUserVo> teamQuery(@RequestBody TeamQueryRequest teamQueryRequest, HttpServletRequest request) {
        if (teamQueryRequest == null) {
            throw new ApiException(ErrorCode.PARAMS_ERROR, "队伍不存在");
        }
        TeamUserVo teams = teamService.teamQuery(teamQueryRequest);
        return ResultUtil.success(teams);
    }

    @PostMapping("/transfer")
    @ApiOperation("转交队长")
    public BaseResponse<Boolean> transferTeam(@RequestBody TransferTeamRequest transferTeamRequest,HttpServletRequest request) {
        if (transferTeamRequest == null) {
            throw new ApiException(ErrorCode.NULL_ERROR, "操作失败");
        }
        UserVo loginUser = userService.getLoginUser(request);
        Boolean transferTeam = teamService.transferTeam(transferTeamRequest, loginUser);
        //删除缓存
        redisTemplate.delete(RedisConstant.TEAM_LIST);
        redisTemplate.delete(RedisConstant.TEAM_USER);
        return ResultUtil.success(transferTeam);
    }


}
