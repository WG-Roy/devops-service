export default ({ formatMessage }) => ({
  data: [{
    deployTimeApps: [],
    deployTimeName: '',
  }],
  fields: [{
    name: 'deployTimeApps',
    type: 'number',
    multiple: true,
    label: formatMessage({ id: 'deploy.envName' }),
  }, {
    name: 'deployTimeName',
    type: 'number',
    label: formatMessage({ id: 'deploy.appName' }),
    defaultValue: 0,
  }],
});
