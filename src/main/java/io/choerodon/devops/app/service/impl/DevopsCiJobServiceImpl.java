package io.choerodon.devops.app.service.impl;

import static io.choerodon.devops.infra.constant.GitOpsConstants.ARTIFACT_NAME_PATTERN;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import io.choerodon.core.exception.CommonException;
import io.choerodon.devops.api.vo.SonarQubeConfigVO;
import io.choerodon.devops.app.service.*;
import io.choerodon.devops.infra.constant.GitOpsConstants;
import io.choerodon.devops.infra.dto.*;
import io.choerodon.devops.infra.dto.gitlab.JobDTO;
import io.choerodon.devops.infra.enums.SonarAuthType;
import io.choerodon.devops.infra.exception.DevopsCiInvalidException;
import io.choerodon.devops.infra.feign.FileFeignClient;
import io.choerodon.devops.infra.feign.SonarClient;
import io.choerodon.devops.infra.feign.operator.GitlabServiceClientOperator;
import io.choerodon.devops.infra.handler.RetrofitHandler;
import io.choerodon.devops.infra.mapper.*;
import io.choerodon.devops.infra.util.GitUserNameUtil;
import io.choerodon.devops.infra.util.MapperUtil;

/**
 * 〈功能简述〉
 * 〈〉
 *
 * @author wanghao
 * @since 2020/4/3 9:24
 */
@Service
public class DevopsCiJobServiceImpl implements DevopsCiJobService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DevopsCiPipelineRecordServiceImpl.class);

    private static final String CREATE_JOB_FAILED = "create.job.failed";
    private static final String DELETE_JOB_FAILED = "delete.job.failed";
    private static final String ERROR_STAGE_ID_IS_NULL = "error.stage.id.is.null";
    private static final String ERROR_PIPELINE_ID_IS_NULL = "error.pipeline.id.is.null";
    private static final String ERROR_GITLAB_PROJECT_ID_IS_NULL = "error.gitlab.project.id.is.null";
    private static final String ERROR_GITLAB_JOB_ID_IS_NULL = "error.gitlab.job.id.is.null";
    private static final String ERROR_UPLOAD_ARTIFACT_TO_MINIO = "error.upload.file.to.minio";
    private static final String ERROR_TOKEN_MISMATCH = "error.app.service.token.mismatch";
    private static final String ERROR_CI_JOB_NON_EXIST = "error.ci.job.non.exist";
    private static final String ERROR_TOKEN_PIPELINE_MISMATCH = "error.app.service.token.pipeline.mismatch";

    private static final String SONAR_KEY = "%s-%s:%s";
    private static final String SONAR = "sonar";

    /**
     * ci的上传的最大文件字节数，默认200 * 1024 * 1024
     */
    @Value("${ci.max.file.bytes:209715200}")
    private Long maxFileSize;

    private DevopsCiJobMapper devopsCiJobMapper;
    private GitlabServiceClientOperator gitlabServiceClientOperator;
    private UserAttrService userAttrService;
    private DevopsCiMavenSettingsMapper devopsCiMavenSettingsMapper;
    private FileFeignClient fileFeignClient;
    private AppServiceService appServiceService;
    private DevopsCiJobArtifactRecordMapper devopsCiJobArtifactRecordMapper;
    private DevopsCiPipelineMapper devopsCiPipelineMapper;
    private DevopsCiPipelineService devopsCiPipelineService;
    private DevopsCiJobRecordService devopsCiJobRecordService;
    private DevopsCiPipelineRecordMapper devopsCiPipelineRecordMapper;

    public DevopsCiJobServiceImpl(DevopsCiJobMapper devopsCiJobMapper,
                                  GitlabServiceClientOperator gitlabServiceClientOperator,
                                  UserAttrService userAttrService,
                                  FileFeignClient fileFeignClient,
                                  DevopsCiJobArtifactRecordMapper devopsCiJobArtifactRecordMapper,
                                  AppServiceService appServiceService,
                                  DevopsCiPipelineMapper devopsCiPipelineMapper,
                                  DevopsCiMavenSettingsMapper devopsCiMavenSettingsMapper,
                                  @Lazy DevopsCiPipelineService devopsCiPipelineService,
                                  DevopsCiJobRecordService devopsCiJobRecordService,
                                  DevopsCiPipelineRecordMapper devopsCiPipelineRecordMapper) {
        this.devopsCiJobMapper = devopsCiJobMapper;
        this.gitlabServiceClientOperator = gitlabServiceClientOperator;
        this.userAttrService = userAttrService;
        this.devopsCiMavenSettingsMapper = devopsCiMavenSettingsMapper;
        this.devopsCiJobArtifactRecordMapper = devopsCiJobArtifactRecordMapper;
        this.appServiceService = appServiceService;
        this.devopsCiPipelineMapper = devopsCiPipelineMapper;
        this.fileFeignClient = fileFeignClient;
        this.devopsCiPipelineService = devopsCiPipelineService;
        this.devopsCiJobRecordService = devopsCiJobRecordService;
        this.devopsCiPipelineRecordMapper = devopsCiPipelineRecordMapper;
    }

    @Override
    @Transactional
    public DevopsCiJobDTO create(DevopsCiJobDTO devopsCiJobDTO) {
        if (devopsCiJobMapper.insertSelective(devopsCiJobDTO) != 1) {
            throw new CommonException(CREATE_JOB_FAILED);
        }
        return devopsCiJobMapper.selectByPrimaryKey(devopsCiJobDTO.getId());
    }

    @Override
    @Transactional
    public void deleteByStageId(Long stageId) {
        if (stageId == null) {
            throw new CommonException(ERROR_STAGE_ID_IS_NULL);
        }

        List<Long> jobIds = listByStageId(stageId).stream().map(DevopsCiJobDTO::getId).collect(Collectors.toList());
        if (!jobIds.isEmpty()) {
            deleteMavenSettingsRecordByJobIds(jobIds);

            DevopsCiJobDTO devopsCiJobDTO = new DevopsCiJobDTO();
            devopsCiJobDTO.setCiStageId(stageId);
            devopsCiJobMapper.delete(devopsCiJobDTO);
        }
    }

    @Override
    public List<DevopsCiJobDTO> listByPipelineId(Long ciPipelineId) {
        if (ciPipelineId == null) {
            throw new CommonException(ERROR_PIPELINE_ID_IS_NULL);
        }
        DevopsCiJobDTO devopsCiJobDTO = new DevopsCiJobDTO();
        devopsCiJobDTO.setCiPipelineId(ciPipelineId);
        return devopsCiJobMapper.select(devopsCiJobDTO);
    }

    @Override
    public List<DevopsCiJobDTO> listByStageId(Long stageId) {
        DevopsCiJobDTO devopsCiJobDTO = new DevopsCiJobDTO();
        devopsCiJobDTO.setCiStageId(Objects.requireNonNull(stageId));
        return devopsCiJobMapper.select(devopsCiJobDTO);
    }

    @Override
    public Boolean sonarConnect(Long projectId, SonarQubeConfigVO sonarQubeConfigVO) {
        if (Objects.isNull(sonarQubeConfigVO)) {
            return false;
        }
        if (Objects.isNull(sonarQubeConfigVO.getSonarUrl())) {
            return false;
        }
        if (SonarAuthType.USERNAME_PWD.value().equals(sonarQubeConfigVO.getAuthType())) {
            SonarClient sonarClient = RetrofitHandler.getSonarClient(
                    sonarQubeConfigVO.getSonarUrl(),
                    SONAR,
                    sonarQubeConfigVO.getUsername(),
                    sonarQubeConfigVO.getPassword());
            try {
                sonarClient.getUser().execute();
            } catch (IOException e) {
                return false;
            }
            return true;
        }
        return true;
    }

    @Override
    public String queryTrace(Long gitlabProjectId, Long jobId) {
        UserAttrDTO userAttrDTO = userAttrService.baseQueryById(GitUserNameUtil.getUserId().longValue());
        return gitlabServiceClientOperator.queryTrace(gitlabProjectId.intValue(), jobId.intValue(), userAttrDTO.getGitlabUserId().intValue());
    }

    @Override
    public void retryJob(Long projectId, Long gitlabProjectId, Long jobId) {
        Assert.notNull(gitlabProjectId, ERROR_GITLAB_PROJECT_ID_IS_NULL);
        Assert.notNull(jobId, ERROR_GITLAB_JOB_ID_IS_NULL);


        UserAttrDTO userAttrDTO = userAttrService.baseQueryById(GitUserNameUtil.getUserId().longValue());
        DevopsCiJobRecordDTO devopsCiJobRecordDTO = devopsCiJobRecordService.queryByGitlabJobId(jobId);
        DevopsCiPipelineRecordDTO devopsCiPipelineRecordDTO = devopsCiPipelineRecordMapper.selectByPrimaryKey(devopsCiJobRecordDTO.getCiPipelineRecordId());
        devopsCiPipelineService.checkUserBranchPushPermission(projectId, userAttrDTO.getGitlabUserId(), gitlabProjectId, devopsCiPipelineRecordDTO.getGitlabTriggerRef());

        JobDTO jobDTO = gitlabServiceClientOperator.retryJob(gitlabProjectId.intValue(), jobId.intValue(), userAttrDTO.getGitlabUserId().intValue());
        // 保存job记录
        try {
            devopsCiJobRecordService.create(devopsCiPipelineRecordDTO.getId(), gitlabProjectId, jobDTO, userAttrDTO.getIamUserId());
        } catch (Exception e) {
            LOGGER.info("update job Records failed， jobid {}.", jobId);
        }
    }

    @Override
    @Transactional
    public void deleteByPipelineId(Long ciPipelineId) {
        if (ciPipelineId == null) {
            throw new CommonException(ERROR_PIPELINE_ID_IS_NULL);
        }

        // 删除maven settings
        deleteMavenSettingsRecordByJobIds(listByPipelineId(ciPipelineId).stream().map(DevopsCiJobDTO::getId).collect(Collectors.toList()));

        DevopsCiJobDTO devopsCiJobDTO = new DevopsCiJobDTO();
        devopsCiJobDTO.setCiPipelineId(ciPipelineId);
        devopsCiJobMapper.delete(devopsCiJobDTO);
    }

    @Override
    public String queryMavenSettings(Long projectId, String appServiceToken, Long jobId, Long sequence) {
        AppServiceDTO appServiceDTO = appServiceService.baseQueryByToken(appServiceToken);
        if (appServiceDTO == null) {
            throw new DevopsCiInvalidException(ERROR_TOKEN_MISMATCH);
        }
        DevopsCiJobDTO devopsCiJobDTO = devopsCiJobMapper.selectByPrimaryKey(jobId);
        if (devopsCiJobDTO == null) {
            throw new DevopsCiInvalidException(ERROR_CI_JOB_NON_EXIST);
        }

        DevopsCiPipelineDTO devopsCiPipelineDTO = devopsCiPipelineMapper.selectByPrimaryKey(devopsCiJobDTO.getCiPipelineId());
        if (devopsCiPipelineDTO == null || !Objects.equals(devopsCiPipelineDTO.getAppServiceId(), appServiceDTO.getId())) {
            throw new DevopsCiInvalidException(ERROR_TOKEN_PIPELINE_MISMATCH);
        }

        return devopsCiMavenSettingsMapper.queryMavenSettings(jobId, sequence);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteMavenSettingsRecordByJobIds(List<Long> jobIds) {
        if (CollectionUtils.isEmpty(jobIds)) {
            return;
        }

        devopsCiMavenSettingsMapper.deleteByJobIds(jobIds);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void uploadArtifact(String token, String commit, Long ciPipelineId, Long ciJobId, String artifactName, MultipartFile file) {
        // 这个方法暂时用不到的字段留待后用
        AppServiceDTO appServiceDTO = appServiceService.baseQueryByToken(token);
        if (appServiceDTO == null) {
            throw new DevopsCiInvalidException(ERROR_TOKEN_MISMATCH);
        }

        if (!ARTIFACT_NAME_PATTERN.matcher(artifactName).matches()) {
            throw new DevopsCiInvalidException("error.artifact.name.invalid", artifactName);
        }
        if (file.getSize() > maxFileSize) {
            throw new DevopsCiInvalidException("error.artifact.too.big", file.getSize(), maxFileSize);
        }
        try {
            // 存到文件服务器的文件名
            String fileName = String.format(GitOpsConstants.CI_JOB_ARTIFACT_NAME_TEMPLATE, ciPipelineId, artifactName);

            DevopsCiJobArtifactRecordDTO recordDTO = devopsCiJobArtifactRecordMapper.queryByPipelineIdAndName(ciPipelineId, artifactName);
            if (recordDTO == null) {
                ResponseEntity<String> response = fileFeignClient.uploadFile(GitOpsConstants.DEV_OPS_CI_ARTIFACT_FILE_BUCKET, fileName, file);
                String artifactUrl;
                if (response == null || StringUtils.isEmpty((artifactUrl = response.getBody()))) {
                    throw new DevopsCiInvalidException(ERROR_UPLOAD_ARTIFACT_TO_MINIO);
                }

                // 插入纪录到数据库
                DevopsCiJobArtifactRecordDTO devopsCiJobArtifactRecordDTO = new DevopsCiJobArtifactRecordDTO(ciPipelineId, ciJobId, artifactName, artifactUrl);
                MapperUtil.resultJudgedInsert(devopsCiJobArtifactRecordMapper, devopsCiJobArtifactRecordDTO, "error.insert.artifact.record");
            } else {
                // 先删除旧的
                fileFeignClient.deleteFile(GitOpsConstants.DEV_OPS_CI_ARTIFACT_FILE_BUCKET, recordDTO.getFileUrl());
                // 上传新的
                ResponseEntity<String> response = fileFeignClient.uploadFile(GitOpsConstants.DEV_OPS_CI_ARTIFACT_FILE_BUCKET, fileName, file);
                String artifactUrl;
                if (response == null || StringUtils.isEmpty((artifactUrl = response.getBody()))) {
                    throw new DevopsCiInvalidException(ERROR_UPLOAD_ARTIFACT_TO_MINIO);
                }

                // 更新数据库纪录
                recordDTO.setFileUrl(artifactUrl);
                recordDTO.setGitlabPipelineId(ciPipelineId);
                recordDTO.setGitlabJobId(ciJobId);
                recordDTO.setName(artifactName);
                devopsCiJobArtifactRecordMapper.updateByPrimaryKeySelective(recordDTO);
            }
        } catch (CommonException e) {
            throw new DevopsCiInvalidException(e.getCode(), e.getCause());
        } catch (DevopsCiInvalidException e) {
            throw e;
        } catch (Exception e) {
            throw new DevopsCiInvalidException(ERROR_UPLOAD_ARTIFACT_TO_MINIO, e);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteArtifactsByGitlabProjectId(List<Long> gitlabPipelineIds) {
        if (CollectionUtils.isEmpty(gitlabPipelineIds)) {
            return;
        }
        List<DevopsCiJobArtifactRecordDTO> artifacts = devopsCiJobArtifactRecordMapper.listByGitlabPipelineIds(gitlabPipelineIds);
        if (!artifacts.isEmpty()) {
            // TODO 要能够文件不存在删除也不报错的接口，融合之后实现
            artifacts.forEach(artifact -> fileFeignClient.deleteFile(GitOpsConstants.DEV_OPS_CI_ARTIFACT_FILE_BUCKET, artifact.getFileUrl()));
            devopsCiJobArtifactRecordMapper.deleteByGitlabPipelineIds(gitlabPipelineIds);
        }
    }

    @Override
    public String queryArtifactUrl(String token, String commit, Long ciPipelineId, Long ciJobId, String artifactName) {
        AppServiceDTO appServiceDTO = appServiceService.baseQueryByToken(token);
        if (appServiceDTO == null) {
            throw new DevopsCiInvalidException(ERROR_TOKEN_MISMATCH);
        }
        DevopsCiJobArtifactRecordDTO recordDTO = devopsCiJobArtifactRecordMapper.queryByPipelineIdAndName(ciPipelineId, artifactName);
        return recordDTO == null ? null : recordDTO.getFileUrl();
    }
}
