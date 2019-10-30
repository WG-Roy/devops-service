import { TARGET_SPECIFIER } from '../Constants';

export default ({ projectId, formatMessage }) => ({
  fields: [
    {
      name: 'notifyTriggerEvent',
      type: 'string',
      label: formatMessage({ id: 'notification.event' }),
      required: true,
      multiple: true,
    },
    {
      name: 'notifyType',
      type: 'string',
      multiple: true,
      label: formatMessage({ id: 'notification.method' }),
      required: true,
    },
    {
      name: 'notifyObject',
      type: 'string',
      label: formatMessage({ id: 'notification.target' }),
      required: true,
    },
    {
      name: 'userRelIds',
      type: 'number',
      multiple: true,
      label: formatMessage({ id: 'notification.target.specifier' }),
      dynamicProps: changeProp,
      lookupAxiosConfig: {
        url: `/devops/v1/projects/${projectId}/users/list_users`,
        method: 'get',
      },
      valueField: 'id',
      textField: 'realName',
    },
  ],
  transport: {
    read: {
      url: '/devops/v1/projects/projectId/notification/id',
      method: 'get',
    },
    submit: ({ data: [data] }) => ({
      url: `/devops/v1/projects/${projectId}/notification`,
      method: 'post',
      data,
    }),
    update: ({ data: [data] }) => ({
      url: `/devops/v1/projects/${projectId}/notification`,
      method: 'put',
      data,
    }),
  },
});


function changeProp({ dataSet, record, name }) {
  return {
    required: record.get('notifyObject') === TARGET_SPECIFIER,
  };
}
