import React, { useContext, Fragment, useState, lazy, Suspense, useCallback, useMemo } from 'react';
import { observer } from 'mobx-react-lite';
import { Tabs, Icon } from 'choerodon-ui';
import { useApplicationStore } from './stores';
import { useResourceStore } from '../../../stores';
import PrefixTitle from '../../components/prefix-title';
import Modals from './modals';

import './index.less';

const { TabPane } = Tabs;

const CipherContent = lazy(() => import('./cipher'));
const MappingContent = lazy(() => import('./mapping'));
const NetContent = lazy(() => import('./net'));

const AppContent = observer(() => {
  const {
    intl: { formatMessage },
    tabs: {
      NET_TAB,
      MAPPING_TAB,
      CIPHER_TAB,
    },
    baseInfoDs,
    appStore,
  } = useApplicationStore();
  const {
    prefixCls,
    intlPrefix,
  } = useResourceStore();

  const handleChange = useCallback((key) => {
    appStore.setTabKey(key);
  }, [appStore]);

  const baseInfo = baseInfoDs.data;

  let title = null;
  if (baseInfo.length) {
    const record = baseInfo[0];
    const name = record.get('name');

    title = <Fragment>
      <Icon type="widgets" />
      <span className={`${prefixCls}-title-text`}>{name}</span>
    </Fragment>;
  }

  return (
    <div className={`${prefixCls}-application`}>
      <Modals />
      <PrefixTitle
        prefixCls={prefixCls}
        fallback={!baseInfo.length}
      >
        {title}
      </PrefixTitle>
      <Tabs
        className={`${prefixCls}-application-tabs`}
        animated={false}
        activeKey={appStore.getTabKey}
        onChange={handleChange}
      >
        <TabPane
          key={NET_TAB}
          tab={formatMessage({ id: `${intlPrefix}.application.tabs.networking` })}
        >
          <Suspense fallback={<div>loading</div>}>
            <NetContent />
          </Suspense>
        </TabPane>
        <TabPane
          key={MAPPING_TAB}
          tab={formatMessage({ id: `${intlPrefix}.application.tabs.mapping` })}
        >
          <Suspense fallback={<div>loading</div>}>
            <MappingContent type={MAPPING_TAB} />
          </Suspense>
        </TabPane>
        <TabPane
          key={CIPHER_TAB}
          tab={formatMessage({ id: `${intlPrefix}.application.tabs.cipher` })}
        >
          <Suspense fallback={<div>loading</div>}>
            <MappingContent type={CIPHER_TAB} />
          </Suspense>
        </TabPane>
      </Tabs>
    </div>
  );
});

export default AppContent;