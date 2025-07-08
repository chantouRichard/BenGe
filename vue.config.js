// module.exports = {
//   devServer: {
//     // https: true, // 启用 HTTPS
//     hot: true,
//     host: '0.0.0.0',
//     historyApiFallback: true,
//     client: {
//       webSocketURL: 'auto://0.0.0.0:8080/ws', // 自动选择 ws/wss
//     },
//     allowedHosts: 'all', // 允许所有 Host（或指定 ngrok 域名）
//     proxy: {
//       '/api': {
//         // target: process.env.VUE_APP_API_BASE_URL || 'https://benge-vision-production.up.railway.app',//
//         target: 'http://localhost:7122',//
//         changeOrigin: true,
//         secure: false,//
//         pathRewrite: { '^/api': '/api' },
//         allowedHosts: 'all'
//       }
//     }
//   }
// };
// module.exports = {
//   devServer: {
//     proxy: {
//       '/api': {
//         target: 'http://localhost:7122',//
//         changeOrigin: true,
//         secure: false,//
//         pathRewrite: { '^/api': '/api' }
//       }
//     }
//   }
// };
module.exports = {
  devServer: {
    proxy: {
      '/api': {
        // target: 'https://9cd1-2001-250-4001-5012-c1e1-eff4-a331-25f4.ngrok-free.app',
        // target: 'https://municipal-donors-expert-class.trycloudflare.com',
        target: 'http://localhost:7122',
        changeOrigin: true,
        secure: false,//
        pathRewrite: { '^/api': '/api' }
      }
    } ,
    client: {
      overlay: {
        runtimeErrors: (error) => {
          // 过滤ResizeObserver错误
          return !error.message.includes('ResizeObserver');
        },
      },
    },
  }
};