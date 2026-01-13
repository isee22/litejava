const http = require('http');

/**
 * 结算服务 - 异步调用 AccountServer，失败自动重试
 */
class SettlementService {
    constructor(accountUrl) {
        this.accountUrl = accountUrl;
        this.retryQueue = [];
        this.scheduleRetry();
    }
    
    submit(roomId, gameType, settlements) {
        this.doSettle({ roomId, gameType, settlements });
    }
    
    async doSettle(task) {
        if (await this.httpPost(task)) {
            console.log(`[Settlement] 成功: ${task.roomId}`);
        } else {
            this.retryQueue.push(task);
        }
    }
    
    async scheduleRetry() {
        while (true) {
            await this.sleep(5000);
            const tasks = this.retryQueue.splice(0);
            for (const task of tasks) {
                this.doSettle(task); // 不 await，并行执行
            }
        }
    }
    
    sleep(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }
    
    httpPost(body) {
        return new Promise(resolve => {
            const data = JSON.stringify(body);
            const url = new URL(this.accountUrl + '/game/settle');
            const req = http.request({
                hostname: url.hostname,
                port: url.port || 80,
                path: url.pathname,
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                timeout: 5000
            }, res => {
                let str = '';
                res.on('data', c => str += c);
                res.on('end', () => {
                    try { resolve(JSON.parse(str).code === 0); }
                    catch { resolve(false); }
                });
            });
            req.on('error', () => resolve(false));
            req.write(data);
            req.end();
        });
    }
}

module.exports = SettlementService;
