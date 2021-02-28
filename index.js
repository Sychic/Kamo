require('dotenv').config({
    path: __dirname + '/.env'
})

const {
    ShardingManager
} = require('discord.js');
const manager = new ShardingManager('./bot.js', {
    token: process.env.TOKEN
});

try {
    manager.spawn();
} catch (error) {
    console.error(("Something went catastrophically wrong!").red);
    console.error(error.message);
}

manager.on('shardCreate', shard => console.log(`Launched shard ${shard.id}`));