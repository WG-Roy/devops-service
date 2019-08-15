import React, { createContext, useContext, useMemo } from 'react';
import { DataSet } from 'choerodon-ui/pro';
import { inject } from 'mobx-react';
import { injectIntl } from 'react-intl';
import BaseInfoDataSet from './BaseInfoDataSet';
import { useResourceStore } from '../../../../stores';

const Store = createContext();

export function useNetworkDetailStore() {
  return useContext(Store);
}

export const StoreProvider = injectIntl(inject('AppState')(
  (props) => {
    const { AppState: { currentMenuType: { id } }, children } = props;
    const {
      intlPrefix,
      intl: { formatMessage },
      resourceStore: { getSelectedMenu: { menuId } },
    } = useResourceStore();
    const baseInfoDs = useMemo(() => new DataSet(BaseInfoDataSet(id, menuId)), [id, menuId]);

    const value = {
      ...props,
      baseInfoDs,
    };

    return (
      <Store.Provider value={value}>
        {children}
      </Store.Provider>
    );
  }
));