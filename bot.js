require('dotenv').config({path:__dirname+'/.env'})
const fs = require('fs');
const Discord = require('discord.js');
global.colors = require("colors");
const verify = require('./verify.js');
const channels = JSON.parse(process.env.CHANNEL);

const client = new Discord.Client();

client.once('ready', () => {
	client.user.setActivity(`${client.users.cache.filter(user => !user.bot ).size} users`, { type: 'WATCHING' });
	console.log((`Mee6 overrated, dyno outdated, ${client.user.username}™ activated`).rainbow);
});

client.on("message",message=>{
	if(message.author.bot) return;
	if(message.guild&&channels.includes(message.channel.id)&&message.member.roles.cache.find(role=>role.name=="Hypixel Verified")==undefined){
		console.log(message.cleanContent);
		verify(message.cleanContent,message);
	}

})

client.login(process.env.TOKEN);