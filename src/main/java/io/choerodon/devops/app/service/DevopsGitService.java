package io.choerodon.devops.app.service;

import java.util.List;
import java.util.Map;

import com.github.pagehelper.PageInfo;
import io.choerodon.base.domain.PageRequest;
import io.choerodon.devops.api.vo.*;
import io.choerodon.devops.infra.dto.DevopsBranchDTO;
import io.choerodon.devops.infra.dto.gitlab.BranchDTO;
import io.choerodon.devops.infra.dto.gitlab.CommitDTO;
import io.choerodon.devops.infra.dto.gitlab.TagDTO;

/**
 * Creator: Runge
 * Date: 2018/7/2
 * Time: 14:39
 * Description:
 */
public interface DevopsGitService {

    /**
     * @param projectId
     * @param appId
     * @return
     */
    String getUrl(Long projectId, Long appId);

    /**
     * @param projectId
     * @param appId
     * @param tag
     * @param ref
     * @param msg
     * @param releaseNotes
     */
    void createTag(Long projectId, Long appId, String tag, String ref, String msg, String releaseNotes);

    /**
     * @param projectId
     * @param appId
     * @param tag
     * @param releaseNotes
     * @return
     */
    TagDTO updateTagRelease(Long projectId, Long appId, String tag, String releaseNotes);

    /**
     * @param projectId
     * @param appId
     * @param tag
     */
    void deleteTag(Long projectId, Long appId, String tag);

    /**
     * 创建分支
     *
     * @param projectId      项目ID
     * @param applicationId  应用ID
     * @param devopsBranchVO 分支
     */
    void createBranch(Long projectId, Long applicationId, DevopsBranchVO devopsBranchVO);

    /**
     * 获取工程下所有分支名
     *
     * @param projectId     项目 ID
     * @param applicationId 应用ID
     * @param pageRequest   分页参数
     * @param params        search param
     * @return Page
     */
    PageInfo<BranchDTO> listBranches(Long projectId, PageRequest pageRequest, Long applicationId, String params);

    /**
     * 查询单个分支
     *
     * @param projectId     项目 ID
     * @param applicationId 应用ID
     * @param branchName    分支名
     * @return BranchUpdateDTO
     */
    DevopsBranchVO queryBranch(Long projectId, Long applicationId, String branchName);

    /**
     * 更新分支关联的问题
     *
     * @param projectId      项目 ID
     * @param applicationId  应用ID
     * @param devopsBranchVO 分支
     */
    void updateBranch(Long projectId, Long applicationId, DevopsBranchVO devopsBranchVO);

    /**
     * 删除分支
     *
     * @param applicationId 应用ID
     * @param branchName    分支名
     */
    void deleteBranch(Long applicationId, String branchName);

    /**
     * 校验分支名唯一性
     *
     * @param projectId     项目id
     * @param applicationId 应用id
     * @param branchName    分支名
     */
    void checkName(Long projectId, Long applicationId, String branchName);

    /**
     * @param projectId
     * @param aplicationId
     * @param state
     * @param pageRequest
     * @return
     */
    Map<String, Object> getMergeRequestList(Long projectId, Long aplicationId, String state, PageRequest pageRequest);

    /**
     * @param projectId
     * @param applicationId
     * @param params
     * @param page
     * @param size
     * @return
     */
    PageInfo<TagVO> getTags(Long projectId, Long applicationId, String params, Integer page, Integer size);

    /**
     * @param projectId
     * @param applicationId
     * @return
     */
    List<io.choerodon.devops.infra.dto.gitlab.TagDTO> getTags(Long projectId, Long applicationId);

    /**
     * @param projectId
     * @param applicationId
     * @param tagName
     * @return
     */
    Boolean checkTag(Long projectId, Long applicationId, String tagName);

    /**
     * @param pushWebHookVO
     * @param token
     */
    void branchSync(PushWebHookVO pushWebHookVO, String token);

    /**
     * @param pushWebHookVO
     */
    void fileResourceSync(PushWebHookVO pushWebHookVO);

    /**
     * @param pushWebHookVO
     * @param token
     */
    void fileResourceSyncSaga(PushWebHookVO pushWebHookVO, String token);

    /**
     * @param branchSagaDTO
     */
    void createBranchBySaga(BranchSagaDTO branchSagaDTO);

    /**
     * 获取Commit
     *
     * @param gitLabProjectId
     * @param commit
     * @param userId
     * @return
     */
    CommitDTO getCommit(Integer gitLabProjectId, String commit, Integer userId);

    BranchDTO baseQueryBranch(Integer gitLabProjectId, String branchName);

    DevopsBranchDTO createDevopsBranch(DevopsBranchDTO devopsBranchDTO);
}
