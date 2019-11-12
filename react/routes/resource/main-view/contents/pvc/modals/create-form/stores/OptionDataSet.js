export default ((projectId) => ({
  autoQuery: true,
  selection: false,
  paging: false,
  transport: {
    read: {
      url: `/devops/v1/projects/${projectId}/pv/pv_available`,
      method: 'post',
      data: {
        params: [],
        searchParam: {
          status: 'Available',
          accessModes: 'ReadWriteMany',
          type: 'NFS',
        },
      },
    },
  },
}));
