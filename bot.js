require('dotenv').config();
const fs = require('fs');
const Discord = require('discord.js');
global.colors = require("colors");

const client = new Discord.Client();

client.once('ready', () => {
	client.user.setActivity(`${client.users.cache.filter(user => !user.bot ).size} users`, { type: 'WATCHING' });
	console.log((`Mee6 overrated, dyno outdated, ${client.user.username}™ activated`).rainbow);
});

client.login(process.env.TOKEN);