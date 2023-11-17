package com.lk.team.controller;

import cn.hutool.core.io.FileUtil;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.lk.team.common.BaseResponse;
import com.lk.team.common.ErrorCode;
import com.lk.team.common.ResultUtil;
import com.lk.team.config.OssConfigProperties;
import com.lk.team.constant.OssConstant;
import com.lk.team.enums.FileUploadBizEnum;
import com.lk.team.exception.ApiException;
import com.lk.team.model.request.UploadFileRequest;
import com.lk.team.model.vo.UserVo;
import com.lk.team.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.Arrays;

/**
 * @Author : lk
 * @create 2023/5/5 18:30
 */
@Api(tags = "文件管理")
@RestController
@RequestMapping("/file")
@Slf4j
public class FileController {
    @Autowired
    private OssConfigProperties oss;
    @Autowired
    private UserService userService;


    @PostMapping("/upload")
    @ApiOperation("阿里云oss文件上传")
    public BaseResponse<String> uploadFile(@RequestPart("file") MultipartFile multipartFile, UploadFileRequest uploadFileRequest, HttpServletRequest request) {
        String biz = uploadFileRequest.getBiz();
        FileUploadBizEnum fileUploadBizEnum = FileUploadBizEnum.getEnumByValue(biz);
        if (fileUploadBizEnum == null) {
            throw new ApiException(ErrorCode.PARAMS_ERROR);
        }

        //校验文件
        validFile(multipartFile, fileUploadBizEnum);

        UserVo loginUser = userService.getLoginUser(request);
        // 文件目录：根据业务、用户来划分
        String uuid = RandomStringUtils.randomAlphanumeric(8);
        String filename = uuid + "-" + multipartFile.getOriginalFilename();
        String filepath = String.format("%s/%s/%s", fileUploadBizEnum.getValue(),loginUser.getId(), filename);


        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(oss.getEndpoint(), oss.getAccessKey(), oss.getSecretKey());
        File file=null;
        try {
            // 上传文件
            file = File.createTempFile(filepath, null);
            multipartFile.transferTo(file);
            ossClient.putObject(oss.getBucket(), filepath, file);
            return ResultUtil.success(OssConstant.COS_HOST+filepath);
        } catch (OSSException oe) {
            log.info("Error Message:" + oe.getErrorMessage());
        } catch (Exception ce) {
            log.info("Error Message:" + ce.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
        return null;

    }

    /**
     * 校验文件
     * @param multipartFile
     * @param fileUploadBizEnum 业务类型
     */
    private void validFile(MultipartFile multipartFile, FileUploadBizEnum fileUploadBizEnum) {
        // 文件大小
        long fileSize = multipartFile.getSize();
        // 文件后缀
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        if (FileUploadBizEnum.USER_AVATAR.equals(fileUploadBizEnum)) {
            //大小校验
            if (fileSize > (1024 * 1024L)) {
                throw new ApiException(ErrorCode.PARAMS_ERROR, "文件大小不能超过 1M");
            }
            //格式校验
            if (!Arrays.asList("jpeg", "jpg", "svg", "png", "webp").contains(fileSuffix)) {
                throw new ApiException(ErrorCode.PARAMS_ERROR, "文件类型错误");
            }
        }
    }
}
