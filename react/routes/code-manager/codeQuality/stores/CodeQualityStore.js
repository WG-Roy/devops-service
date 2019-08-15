import { observable, action, computed } from 'mobx';
import { axios, store } from '@choerodon/master';
import { handleProptError } from '../../../../utils';

@store('CodeQualityStore')
class CodeQualityStore {
  @observable data = null;

  @observable appData = [];

  @observable loading = false;

  @computed get getData() {
    return this.data;
  }

  @action setData(data) {
    this.data = data;
  }

  @action changeLoading(flag) {
    this.loading = flag;
  }

  @computed get getLoading() {
    return this.loading;
  }

  /**
   ** 查询代码质量数据
   */
  loadData = (projectId, appId) => {
    this.changeLoading(true);
    return axios.get(`/devops/v1/projects/${projectId}/app_service/${appId}/sonarqube`)
      .then((data) => {
        const res = handleProptError(data);
        if (res) {
          this.setData(res);
        }
        this.changeLoading(false);
      });
  };
}

const codeQualityStore = new CodeQualityStore();
export default codeQualityStore;