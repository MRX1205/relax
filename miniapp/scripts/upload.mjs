import ci from 'miniprogram-ci';
import path from 'path';
import fs from 'fs';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const appid = process.env.WX_APP_ID || 'wxee1086d1aae0e823';
const privateKeyPath = process.env.WX_PRIVATE_KEY_PATH || path.resolve(__dirname, `../private.${appid}.key`);
const version = process.env.WX_VERSION || '0.1.0';
const desc = process.env.WX_DESC || `Auto CI upload at ${new Date().toLocaleString('zh-CN', { timeZone: 'Asia/Shanghai' })}`;

if (!fs.existsSync(privateKeyPath)) {
  console.error(`❌ 未找到微信小程序上传私钥文件: ${privateKeyPath}`);
  console.error('【操作指引】');
  console.error('1. 登录微信公众平台 (mp.weixin.qq.com)');
  console.error('2. 进入：开发 -> 开发管理 -> 开发设置 -> 小程序代码上传');
  console.error('3. 点击【生成/重置代码上传密钥】并下载文件');
  console.error(`4. 将下载的文件保存为: ${privateKeyPath}，或配置环境变量 WX_PRIVATE_KEY_PATH`);
  console.error('5. 同时在后台关闭“IP白名单”限制（或将当前 IP 添加至白名单）');
  process.exit(1);
}

const project = new ci.Project({
  appid,
  type: 'miniProgram',
  projectPath: path.resolve(__dirname, '..'),
  privateKeyPath,
  ignores: ['node_modules/**/*', 'tests/**/*', 'scripts/**/*'],
});

async function main() {
  console.log(`🚀 开始上传小程序代码到微信公众平台...`);
  console.log(`AppID: ${appid}`);
  console.log(`版本号: ${version}`);
  console.log(`版本描述: ${desc}`);

  const uploadResult = await ci.upload({
    project,
    version,
    desc,
    setting: {
      es6: true,
      es7: true,
      minify: true,
      minifyWXML: true,
      minifyWXSS: true,
      autoPrefixWXSS: true,
    },
    onProgressUpdate: (task) => {
      if (typeof task === 'string') {
        console.log(`[CI] ${task}`);
      } else if (task && task.message) {
        console.log(`[CI] ${task.message}`);
      }
    },
  });

  console.log('🎉 小程序代码上传成功！', uploadResult);

  const previewTarget = path.resolve(__dirname, '../preview.jpg');
  console.log(`📱 正在生成体验版/开发版二维码至: ${previewTarget}...`);
  try {
    await ci.preview({
      project,
      desc,
      setting: { es6: true, minify: true },
      qrcodeFormat: 'image',
      qrcodeOutputDest: previewTarget,
      onProgressUpdate: () => {},
    });
    console.log(`✅ 二维码生成成功！文件路径: ${previewTarget}`);
  } catch (e) {
    console.log(`ℹ️ 生成预览二维码跳过或失败: ${e.message}`);
  }
}

main().catch(err => {
  console.error('❌ 上传执行失败:', err);
  process.exit(1);
});
