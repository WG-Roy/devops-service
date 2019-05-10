package io.choerodon.devops.infra.feign.fallback;

import java.util.Map;

import io.choerodon.devops.api.dto.sonar.Bug;
import io.choerodon.devops.api.dto.sonar.SonarComponent;
import io.choerodon.devops.api.dto.sonar.SonarTables;
import io.choerodon.devops.api.dto.sonar.Vulnerability;
import io.choerodon.devops.infra.feign.SonarClient;
import retrofit2.Call;

/**
 * Created by Sheep on 2019/5/6.
 */
public class SonarClientFallback implements SonarClient {

    @Override
    public Call<SonarComponent> getSonarComponet(Map<String, String> maps) {
        return null;
    }

    @Override
    public Call<Bug> getBugs(Map<String, String> maps) {
        return null;
    }

    @Override
    public Call<Vulnerability> getVulnerability(Map<String, String> maps) {
        return null;
    }

    @Override
    public Call<Bug> getNewBugs(Map<String, String> maps) {
        return null;
    }

    @Override
    public Call<Vulnerability> getNewVulnerability(Map<String, String> maps) {
        return null;
    }

    @Override
    public Call<SonarTables> getSonarTables(Map<String, String> maps) {
        return null;
    }


}