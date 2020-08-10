require('dotenv').config();
const fs = require('fs');
const mongodb = require('mongodb');
const Discord = require('discord.js');
global.colors = require("colors");
const verify = require('./verify.js');
const channels = JSON.parse(process.env.CHANNEL);

const client = new Discord.Client();
const mongo = new mongodb.MongoClient(process.env.DB, {
	useUnifiedTopology: true,
	useNewUrlParser: true,
	});
try {
	mongo.connect();
} catch (error) {
	console.error(error);
}
module.exports.mongo = mongo;

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