import React, { useCallback, Fragment } from 'react';
import { Page, Content, Header, Permission, Action, Breadcrumb, Choerodon } from '@choerodon/boot';
import { Table, Modal, Spin } from 'choerodon-ui/pro';
import { Button } from 'choerodon-ui';
import { FormattedMessage } from 'react-intl';
import { withRouter } from 'react-router-dom';
import { observer } from 'mobx-react-lite';
import omit from 'lodash/omit';
import { usePVManagerStore } from './stores';
import CreateForm from './modals/create-form';
import PermissionManager from './modals/permission-mananger';
import StatusTag from '../../components/status-tag';
import { handlePromptError } from '../../utils';

import './index.less';

const { Column } = Table;
const modalKey1 = Modal.key();
const modalKey2 = Modal.key();
const deleteKey = Modal.key();
const modalStyle1 = {
  width: 380,
};
const modalStyle2 = {
  width: 'calc(100vw - 3.52rem)',
};
const statusStyle = {
  width: 56,
  marginRight: 8,
  height: '.16rem',
  lineHeight: '.16rem',
};

const AppService = withRouter(observer((props) => {
  const {
    intl: { formatMessage },
    AppState: { currentMenuType: { projectId } },
    intlPrefix,
    prefixCls,
    permissions,
    listDs,
    pvStore,
  } = usePVManagerStore();

  function refresh() {
    listDs.query();
  }
  
  function renderName({ value, record }) {
    const status = record.get('status');
    let color = 'rgba(0, 0, 0, 0.26)';
    switch (status) {
      case 'Pending':
      case 'Operating':
      case 'Terminating':
        color = '#4D90FE';
        break;
      case 'Available':
        color = '#00BFA5';
        break;
      case 'Bound':
        color = '#FFB100';
        break;
      case 'Released':
        color = 'rgba(0, 0, 0, 0.26)';
        break;
      case 'Failed':
        color = '#F44336';
        break;
      default:
    }
    return (
      <Fragment>
        <StatusTag
          name={status}
          color={color}
          style={statusStyle}
        />
        <span>{value}</span>
      </Fragment>

    );
  }

  function renderActions({ record }) {
    const actionData = {
      permission: {
        service: ['devops-service.devops-pv.queryById'],
        text: formatMessage({ id: `${intlPrefix}.permission` }),
        action: openPermission,
      },
      delete: {
        service: ['devops-service.devops-pv.deletePv'],
        text: formatMessage({ id: 'delete' }),
        action: handleDelete,
      },
    };
    const status = record.get('status');
    let data;
    switch (status) {
      case 'Available':
        if (record.get('pvcName')) {
          data = [actionData.permission];
        } else {
          data = [actionData.permission, actionData.delete];
        }
        break;
      case 'Released':
      case 'Failed':
        if (!record.get('pvcName')) {
          data = [actionData.delete];
        }
        break;
      default:
    }
    return data && <Action data={data} />;
  }

  function openCreate() {
    Modal.open({
      key: modalKey1,
      style: modalStyle1,
      drawer: true,
      title: formatMessage({ id: `${intlPrefix}.create` }),
      children: <CreateForm
        intlPrefix={intlPrefix}
        prefixCls={prefixCls}
        refresh={refresh}
      />,
      okText: formatMessage({ id: 'create' }),
    });
  }

  async function openPermission() {
    const record = listDs.current;
    Modal.open({
      key: modalKey2,
      style: modalStyle2,
      drawer: true,
      title: formatMessage({ id: `${intlPrefix}.permission` }),
      children: <PermissionManager
        intlPrefix={intlPrefix}
        prefixCls={prefixCls}
        refresh={refresh}
        pvId={record.get('id')}
      />,
      okText: formatMessage({ id: 'save' }),
    });
  }

  async function handleDelete() {
    const record = listDs.current;
    const modalProps = {
      title: formatMessage({ id: `${intlPrefix}.delete.title` }, { name: record.get('name') }),
      children: formatMessage({ id: `${intlPrefix}.delete.des` }),
      okText: formatMessage({ id: 'delete' }),
      okProps: { color: 'red' },
      cancelProps: { color: 'dark' },
    };
    listDs.delete(record, modalProps);
  }

  return (
    <Page
      service={permissions}
    >
      <Header>
        <Permission
          service={['devops-service.devops-pv.createPv']}
        >
          <Button
            icon="playlist_add"
            onClick={openCreate}
          >
            <FormattedMessage id={`${intlPrefix}.create`} />
          </Button>
        </Permission>
        <Button
          icon="refresh"
          onClick={refresh}
        >
          <FormattedMessage id="refresh" />
        </Button>
      </Header>
      <Breadcrumb />
      <Content className={`${prefixCls}-content`}>
        <Table
          dataSet={listDs}
          border={false}
          queryBar="bar"
          className={`${prefixCls}-table`}
        >
          <Column name="name" renderer={renderName} sortable />
          <Column renderer={renderActions} width={70} />
          <Column name="description" sortable />
          <Column name="clusterName" />
          <Column name="type" width={100} />
          <Column name="pvcName" />
          <Column name="accessModes" width={140} />
          <Column name="requestResource" width={100} />
        </Table>
      </Content>
    </Page>
  );
}));

export default AppService;
