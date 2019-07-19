package io.choerodon.devops.app.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.choerodon.core.convertor.ConvertHelper;
import io.choerodon.core.exception.CommonException;
import io.choerodon.devops.api.validator.DevopsSecretValidator;
import io.choerodon.devops.api.vo.SecretRepDTO;
import io.choerodon.devops.api.vo.SecretReqDTO;
import io.choerodon.devops.app.service.*;
import io.choerodon.devops.infra.dto.*;
import io.choerodon.devops.infra.enums.CommandStatus;
import io.choerodon.devops.infra.enums.HelmObjectKind;
import io.choerodon.devops.infra.enums.ObjectType;
import io.choerodon.devops.infra.enums.SecretStatus;
import io.choerodon.devops.infra.feign.operator.GitlabServiceClientOperator;
import io.choerodon.devops.infra.gitops.ResourceConvertToYamlHandler;
import io.choerodon.devops.infra.gitops.ResourceFileCheckHandler;
import io.choerodon.devops.infra.handler.ClusterConnectionHandler;
import io.choerodon.devops.infra.mapper.DevopsSecretMapper;
import io.choerodon.devops.infra.util.*;
import io.kubernetes.client.models.V1ObjectMeta;
import io.kubernetes.client.models.V1Secret;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


/**
 * Created by n!Ck
 * Date: 18-12-4
 * Time: 上午9:52
 * Description:
 */

@Component
public class DevopsSecretServiceImpl implements DevopsSecretService {

    private static final String SECRET = "Secret";
    private static final String CREATE = "create";
    private static final String UPDATE = "update";
    private static final String DELETE = "delete";
    private static final String SECRET_KIND = "secret";

    private Gson gson = new Gson();

    @Autowired
    private ClusterConnectionHandler clusterConnectionHandler;
    @Autowired
    private DevopsEnvCommandService devopsEnvCommandService;
    @Autowired
    private UserAttrService userAttrService;
    @Autowired
    private DevopsEnvUserPermissionService devopsEnvUserPermissionService;
    @Autowired
    private DevopsEnvFileResourceService devopsEnvFileResourceService;
    @Autowired
    private GitlabServiceClientOperator gitlabServiceClientOperator;
    @Autowired
    private ResourceFileCheckHandler resourceFileCheckHandler;
    @Autowired
    private DevopsEnvironmentService devopsEnvironmentService;
    @Autowired
    private DevopsApplicationResourceService devopsApplicationResourceService;
    @Autowired
    private DevopsSecretMapper devopsSecretMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SecretRepDTO createOrUpdate(SecretReqDTO secretReqDTO) {
        if (secretReqDTO.getValue() == null || secretReqDTO.getValue().size() == 0) {
            throw new CommonException("error.secret.value.is.null");
        }
        DevopsEnvironmentDTO devopsEnvironmentDTO = devopsEnvironmentService.baseQueryById(secretReqDTO.getEnvId());
        UserAttrDTO userAttrDTO = userAttrService.baseQueryById(TypeUtil.objToLong(GitUserNameUtil.getUserId()));

        //校验环境相关信息
        devopsEnvironmentService.checkEnv(devopsEnvironmentDTO, userAttrDTO);
        // 处理secret对象
        DevopsSecretDTO devopsSecretDTO = handleSecret(secretReqDTO);
        // 初始化V1Secret对象
        V1Secret v1Secret = initV1Secret(devopsSecretDTO);
        // 更新操作如果key-value没有改变
        if (UPDATE.equals(secretReqDTO.getType())) {
            //更新secret的时候校验gitops库文件是否存在,处理部署secret时，由于没有创gitops文件导致的部署失败
            resourceFileCheckHandler.check(devopsEnvironmentDTO, secretReqDTO.getId(), secretReqDTO.getName(), SECRET);

            DevopsSecretDTO oldSecretDTO = baseQueryByEnvIdAndName(secretReqDTO.getEnvId(), secretReqDTO.getName());
            Map<String, String> secretMaps = gson.fromJson(oldSecretDTO.getValue(), new TypeToken<Map<String, String>>() {
            }.getType());
            oldSecretDTO.setValueMap(secretMaps);
            Map<String, String> oldMap = new HashMap<>();
            for (Map.Entry<String, String> e : oldSecretDTO.getValueMap().entrySet()) {
                oldMap.put(e.getKey(), Base64Util.getBase64DecodedString(e.getValue()));
            }
            if (oldMap.equals(secretReqDTO.getValue())) {
                baseUpdate(devopsSecretDTO);
                return ConvertHelper.convert(devopsSecretDTO, SecretRepDTO.class);
            }
        }
        DevopsEnvCommandDTO devopsEnvCommandE = initDevopsEnvCommandDTO(secretReqDTO.getType());

        // 在gitops库处理secret文件
        operateEnvGitLabFile(TypeUtil.objToInteger(devopsEnvironmentDTO.getGitlabEnvProjectId()), v1Secret, devopsSecretDTO,
                devopsEnvCommandE, CREATE.equals(secretReqDTO.getType()), userAttrDTO, secretReqDTO.getAppId());
        return ConvertUtils.convertObject(baseQuery(devopsSecretDTO.getId()), SecretRepDTO.class);
    }

    private DevopsSecretDTO handleSecret(SecretReqDTO secretReqDTO) {
        if (CREATE.equals(secretReqDTO.getType())) {
            // 校验secret名字合法性和环境下唯一性
            DevopsSecretValidator.checkName(secretReqDTO.getName());
            baseCheckName(secretReqDTO.getName(), secretReqDTO.getEnvId());
        }
        // 校验key-name
        if (!secretReqDTO.getType().equals("kubernetes.io/dockerconfigjson")) {
            DevopsSecretValidator.checkKeyName(secretReqDTO.getValue().keySet());
        }

        DevopsSecretDTO devopsSecretDTO = ConvertHelper.convert(secretReqDTO, DevopsSecretDTO.class);
        devopsSecretDTO.setValueMap(secretReqDTO.getValue());
        devopsSecretDTO.setStatus(SecretStatus.OPERATING.getStatus());

        return devopsSecretDTO;
    }

    private V1Secret initV1Secret(DevopsSecretDTO devopsSecretDTO) {
        V1Secret secret = new V1Secret();
        secret.setApiVersion("v1");
        secret.setKind(SECRET);
        V1ObjectMeta metadata = new V1ObjectMeta();
        metadata.setName(devopsSecretDTO.getName());
        secret.setMetadata(metadata);
        secret.setType("Opaque");
        secret.setStringData(devopsSecretDTO.getValueMap());
        return secret;
    }

    private void operateEnvGitLabFile(Integer gitlabEnvGroupProjectId, V1Secret v1Secret, DevopsSecretDTO devopsSecretDTO,
                                      DevopsEnvCommandDTO devopsEnvCommandDTO, Boolean isCreate, UserAttrDTO userAttrDTO, Long appId) {

        DevopsEnvironmentDTO devopsEnvironmentDTO = devopsEnvironmentService.baseQueryById(devopsSecretDTO.getEnvId());

        //操作secret数据库
        if (isCreate) {
            Long secretId = baseCreate(devopsSecretDTO).getId();
            //创建应用资源关系
            if (appId != null) {
                DevopsApplicationResourceDTO applicationResourceDTO = new DevopsApplicationResourceDTO();
                applicationResourceDTO.setAppId(appId);
                applicationResourceDTO.setResourceType(ObjectType.SERVICE.getType());
                applicationResourceDTO.setResourceId(secretId);
                devopsApplicationResourceService.baseCreate(applicationResourceDTO);
            }
            devopsEnvCommandDTO.setObjectId(secretId);
            devopsSecretDTO.setId(secretId);
            devopsSecretDTO.setCommandId(devopsEnvCommandService.baseCreate(devopsEnvCommandDTO).getId());
            baseUpdate(devopsSecretDTO);
        } else {
            devopsEnvCommandDTO.setObjectId(devopsSecretDTO.getId());
            devopsSecretDTO.setCommandId(devopsEnvCommandService.baseCreate(devopsEnvCommandDTO).getId());
            baseUpdate(devopsSecretDTO);
        }

        // 判断当前容器目录下是否存在环境对应的gitops文件目录，不存在则克隆
        String path = clusterConnectionHandler.handDevopsEnvGitRepository(devopsEnvironmentDTO.getProjectId(), devopsEnvironmentDTO.getCode(), devopsEnvironmentDTO.getEnvIdRsa());

        ResourceConvertToYamlHandler<V1Secret> resourceConvertToYamlHandler = new ResourceConvertToYamlHandler<>();
        resourceConvertToYamlHandler.setType(v1Secret);
        resourceConvertToYamlHandler.operationEnvGitlabFile("sct-" + devopsSecretDTO.getName(), gitlabEnvGroupProjectId,
                isCreate ? CREATE : UPDATE, userAttrDTO.getGitlabUserId(), devopsSecretDTO.getId(), SECRET, null, false,
                devopsSecretDTO.getEnvId(), path);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteSecret(Long envId, Long secretId) {

        UserAttrDTO userAttrDTO = userAttrService.baseQueryById(TypeUtil.objToLong(GitUserNameUtil.getUserId()));

        DevopsEnvironmentDTO devopsEnvironmentDTO = devopsEnvironmentService.baseQueryById(envId);

        //校验环境相关信息
        devopsEnvironmentService.checkEnv(devopsEnvironmentDTO, userAttrDTO);

        DevopsEnvCommandDTO devopsEnvCommandDTO = initDevopsEnvCommandDTO(DELETE);

        // 更新secret
        devopsEnvCommandDTO.setObjectId(secretId);
        DevopsSecretDTO devopsSecretDTO = baseQuery(secretId);
        devopsSecretDTO.setCommandId(devopsEnvCommandService.baseCreate(devopsEnvCommandDTO).getId());
        devopsSecretDTO.setStatus(SecretStatus.OPERATING.getStatus());
        baseUpdate(devopsSecretDTO);

        //判断当前容器目录下是否存在环境对应的gitops文件目录，不存在则克隆
        String path = clusterConnectionHandler.handDevopsEnvGitRepository(devopsEnvironmentDTO.getProjectId(), devopsEnvironmentDTO.getCode(), devopsEnvironmentDTO.getEnvIdRsa());

        // 查询改对象所在文件中是否含有其它对象
        DevopsEnvFileResourceDTO devopsEnvFileResourceDTO = devopsEnvFileResourceService
                .baseQueryByEnvIdAndResourceId(devopsEnvironmentDTO.getId(), secretId, SECRET);
        if (devopsEnvFileResourceDTO == null) {
            baseDelete(secretId);
            devopsApplicationResourceService.baseDeleteByResourceIdAndType(secretId, ObjectType.SECRET.getType());
            if (gitlabServiceClientOperator.getFile(TypeUtil.objToInteger(devopsEnvironmentDTO.getGitlabEnvProjectId()), "master",
                    "sct-" + devopsSecretDTO.getName() + ".yaml")) {
                gitlabServiceClientOperator.deleteFile(
                        TypeUtil.objToInteger(devopsEnvironmentDTO.getGitlabEnvProjectId()),
                        "sct-" + devopsSecretDTO.getName() + ".yaml",
                        "DELETE FILE",
                        TypeUtil.objToInteger(userAttrDTO.getGitlabUserId()));
            }
            return true;
        } else {
            if (!gitlabServiceClientOperator.getFile(TypeUtil.objToInteger(devopsEnvironmentDTO.getGitlabEnvProjectId()), "master",
                    devopsEnvFileResourceDTO.getFilePath())) {
                baseDelete(secretId);
                devopsApplicationResourceService.baseDeleteByResourceIdAndType(secretId, ObjectType.SECRET.getType());
                devopsEnvFileResourceService.baseDeleteById(devopsEnvFileResourceDTO.getId());
                return true;
            }
        }
        List<DevopsEnvFileResourceDTO> devopsEnvFileResourceDTOS = devopsEnvFileResourceService
                .baseQueryByEnvIdAndPath(devopsEnvironmentDTO.getId(), devopsEnvFileResourceDTO.getFilePath());

        // 如果对象所在文件只有一个对象，则直接删除文件,否则把对象从文件中去掉，更新文件
        if (devopsEnvFileResourceDTOS.size() == 1) {
            if (gitlabServiceClientOperator.getFile(TypeUtil.objToInteger(devopsEnvironmentDTO.getGitlabEnvProjectId()), "master",
                    devopsEnvFileResourceDTO.getFilePath())) {
                gitlabServiceClientOperator.deleteFile(TypeUtil.objToInteger(devopsEnvironmentDTO.getGitlabEnvProjectId()),
                        devopsEnvFileResourceDTO.getFilePath(), "DELETE FILE",
                        TypeUtil.objToInteger(userAttrDTO.getGitlabUserId()));
            }
        } else {
            ResourceConvertToYamlHandler<V1Secret> resourceConvertToYamlHandler = new ResourceConvertToYamlHandler<>();
            V1Secret v1Secret = new V1Secret();
            V1ObjectMeta v1ObjectMeta = new V1ObjectMeta();
            v1ObjectMeta.setName(devopsSecretDTO.getName());
            v1Secret.setMetadata(v1ObjectMeta);
            resourceConvertToYamlHandler.setType(v1Secret);
            Integer projectId = TypeUtil.objToInteger(devopsEnvironmentDTO.getGitlabEnvProjectId());
            resourceConvertToYamlHandler.operationEnvGitlabFile(null, projectId, DELETE, userAttrDTO.getGitlabUserId(), secretId,
                    SECRET, null, false, devopsEnvironmentDTO.getId(), path);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSecretByGitOps(Long secretId) {
        DevopsSecretDTO devopsSecretDTO = baseQuery(secretId);
        DevopsEnvironmentDTO devopsEnvironmentDTO = devopsEnvironmentService.baseQueryById(devopsSecretDTO.getEnvId());
        clusterConnectionHandler.checkEnvConnection(devopsEnvironmentDTO.getClusterId());
        devopsEnvCommandService.baseListByObject(HelmObjectKind.SECRET.toValue(), devopsSecretDTO.getId()).forEach(t -> devopsEnvCommandService.baseDeleteByEnvCommandId(t));
        baseDelete(secretId);
        devopsApplicationResourceService.baseDeleteByResourceIdAndType(secretId, ObjectType.SECRET.getType());
    }

    @Override
    public void addSecretByGitOps(SecretReqDTO secretReqDTO, Long userId) {
        DevopsEnvironmentDTO devopsEnvironmentDTO = devopsEnvironmentService.baseQueryById(secretReqDTO.getEnvId());
        //校验环境是否链接
        clusterConnectionHandler.checkEnvConnection(devopsEnvironmentDTO.getClusterId());
        // 处理secret对象
        DevopsSecretDTO devopsSecretDTO = handleSecret(secretReqDTO);
        // 创建secret
        Long secretId = baseCreate(devopsSecretDTO).getId();
        DevopsEnvCommandDTO devopsEnvCommandDTO = new DevopsEnvCommandDTO();
        devopsEnvCommandDTO.setCommandType(CREATE);
        devopsEnvCommandDTO.setStatus(devopsSecretDTO.getStatus());
        devopsEnvCommandDTO.setObjectId(secretId);
        devopsEnvCommandDTO.setObject("secret");
        devopsEnvCommandDTO.setCreatedBy(userId);
        devopsSecretDTO.setCommandId(devopsEnvCommandService.baseCreate(devopsEnvCommandDTO).getId());
        devopsSecretDTO.setId(secretId);
        devopsSecretDTO.setEnvId(devopsEnvironmentDTO.getId());
        baseUpdate(devopsSecretDTO);
    }

    @Override
    public void updateDevopsSecretByGitOps(Long projectId, Long id, SecretReqDTO secretReqDTO, Long userId) {

        DevopsEnvironmentDTO devopsEnvironmentDTO = devopsEnvironmentService.baseQueryById(secretReqDTO.getEnvId());
        //校验环境是否链接
        clusterConnectionHandler.checkEnvConnection(devopsEnvironmentDTO.getClusterId());

        DevopsSecretDTO oldSecretDTO = baseQueryByEnvIdAndName(secretReqDTO.getEnvId(), secretReqDTO.getName());
        if (oldSecretDTO.getValue().equals(secretReqDTO.getValue())) {
            return;
        }
        // 处理secret对象
        DevopsSecretDTO devopsSecretE = handleSecret(secretReqDTO);
        DevopsEnvCommandDTO devopsEnvCommandDTO = initDevopsEnvCommandDTO(UPDATE);
        // 更新secret对象
        devopsEnvCommandDTO.setObjectId(id);
        devopsEnvCommandDTO.setCreatedBy(userId);
        devopsSecretE.setCommandId(devopsEnvCommandService.baseCreate(devopsEnvCommandDTO).getId());
        baseUpdate(devopsSecretE);
    }

    private DevopsEnvCommandDTO initDevopsEnvCommandDTO(String type) {
        DevopsEnvCommandDTO devopsEnvCommandDTO = new DevopsEnvCommandDTO();
        devopsEnvCommandDTO.setCommandType(type);
        devopsEnvCommandDTO.setObject(ObjectType.SECRET.getType());
        devopsEnvCommandDTO.setStatus(CommandStatus.OPERATING.getStatus());
        return devopsEnvCommandDTO;
    }

    @Override
    public PageInfo<SecretRepDTO> pageByOption(Long envId, PageRequest pageRequest, String params, Long appId) {
        return ConvertUtils.convertPage(basePageByOption(envId, pageRequest, params, appId), SecretRepDTO.class);
    }

    @Override
    public SecretRepDTO querySecret(Long secretId) {
        return ConvertUtils.convertObject(baseQuery(secretId), SecretRepDTO.class);
    }

    @Override
    public void checkName(Long envId, String name) {
        DevopsSecretValidator.checkName(name);
        baseCheckName(name, envId);
    }

    public DevopsSecretDTO baseCreate(DevopsSecretDTO devopsSecretDTO) {
        if (devopsSecretMapper.insert(devopsSecretDTO) != 1) {
            throw new CommonException("error.secret.insert");
        }
        return devopsSecretDTO;
    }

    public void baseUpdate(DevopsSecretDTO devopsSecretDTO) {
        DevopsSecretDTO oldDevopsSecretDTO = devopsSecretMapper.selectByPrimaryKey(devopsSecretDTO.getId());
        if (oldDevopsSecretDTO == null) {
            throw new CommonException("secret.not.exists");
        }
        devopsSecretDTO.setObjectVersionNumber(oldDevopsSecretDTO.getObjectVersionNumber());
        if (devopsSecretMapper.updateByPrimaryKeySelective(devopsSecretDTO) != 1) {
            throw new CommonException("secret.update.error");
        }
    }

    public void baseDelete(Long secretId) {
        devopsSecretMapper.deleteByPrimaryKey(secretId);
        devopsSecretMapper.delete(new DevopsSecretDTO(secretId));
    }

    public void baseCheckName(String name, Long envId) {
        DevopsSecretDTO devopsSecretDTO = new DevopsSecretDTO();
        devopsSecretDTO.setName(name);
        devopsSecretDTO.setEnvId(envId);
        if (devopsSecretMapper.selectOne(devopsSecretDTO) != null) {
            throw new CommonException("error.secret.name.already.exists");
        }
    }

    @Override
    public DevopsSecretDTO baseQuery(Long secretId) {
        return devopsSecretMapper.selectById(secretId);
    }

    public DevopsSecretDTO baseQueryByEnvIdAndName(Long envId, String name) {
        DevopsSecretDTO devopsSecretDTO = new DevopsSecretDTO();
        devopsSecretDTO.setEnvId(envId);
        devopsSecretDTO.setName(name);
        return devopsSecretMapper.selectOne(devopsSecretDTO);
    }

    public PageInfo<DevopsSecretDTO> basePageByOption(Long envId, PageRequest pageRequest, String params, Long appId) {
        Map maps = gson.fromJson(params, Map.class);
        Map<String, Object> searchParamMap = TypeUtil.cast(maps.get(TypeUtil.SEARCH_PARAM));
        String paramMap = TypeUtil.cast(maps.get(TypeUtil.PARAM));
        PageInfo<DevopsSecretDTO> devopsSecretDTOPageInfo = PageHelper
                .startPage(pageRequest.getPage(), pageRequest.getSize(), PageRequestUtil.getOrderBy(pageRequest)).doSelectPageInfo(() -> devopsSecretMapper.listByOption(envId, searchParamMap, paramMap, appId));
        return devopsSecretDTOPageInfo;
    }

    public List<DevopsSecretDTO> baseListByEnv(Long envId) {
        DevopsSecretDTO devopsSecretDTO = new DevopsSecretDTO();
        devopsSecretDTO.setEnvId(envId);
        return devopsSecretMapper.select(devopsSecretDTO);
    }

    private Map<String, String> getEncodedSecretMaps(SecretReqDTO secretReqDTO) {
        Map<String, String> encodedSecretMaps = new HashMap<>();
        if (!secretReqDTO.getValue().isEmpty()) {
            for (Map.Entry<String, String> e : secretReqDTO.getValue().entrySet()) {
                if (!e.getKey().equals(".dockerconfigjson")) {
                    encodedSecretMaps.put(e.getKey(), Base64Util.getBase64EncodedString(e.getValue()));
                } else {
                    encodedSecretMaps.put(e.getKey(), e.getValue());
                }
            }
        }
        return encodedSecretMaps;
    }

}
