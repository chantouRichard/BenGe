// module.exports = {
//   devServer: {
//     hot: true,
//     host: '0.0.0.0',
//     historyApiFallback: true,
//     client: {
//       webSocketURL: 'auto://0.0.0.0:8080/ws', // 这是正确的WebSocket配置位置
//     },
//     allowedHosts: 'all',
//     proxy: {
//       '/api': {
//         target: 'http://localhost:7122',
//         changeOrigin: true,
//         secure: false,
//         pathRewrite: { '^/api': '/api' },
//         // 确保流式传输正常工作
//         onProxyRes: function(proxyRes) {
//           proxyRes.headers['connection'] = 'keep-alive';
//           proxyRes.headers['cache-control'] = 'no-cache';
//           delete proxyRes.headers['content-length'];
//         },

//       }
//     },
//     // 正确的WebSocket服务器配置
//     webSocketServer: {
//       type: 'ws',
//       options: {
//         path: '/ws'
//       }
//     },
//     // 禁用开发服务器的压缩
//     compress: false
//   }
// }

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
        target: 'https://9cd1-2001-250-4001-5012-c1e1-eff4-a331-25f4.ngrok-free.app',
        changeOrigin: true,
        secure: false,//
        pathRewrite: { '^/api': '/api' },

        onProxyRes: function(proxyRes) {
          proxyRes.headers['connection'] = 'keep-alive';
          proxyRes.headers['cache-control'] = 'no-cache';
          delete proxyRes.headers['content-length'];
        },
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