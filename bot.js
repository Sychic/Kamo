require('dotenv').config({path:__dirname+'/.env'})
const fs = require('fs');
const Discord = require('discord.js');
global.colors = require("colors");
const verify = require('./verify.js');
const channels = JSON.parse(process.env.CHANNEL);

const client = new Discord.Client();

client.commands = new Discord.Collection();

let dirlist = fs.readdirSync('./commands',{ withFileTypes: true })
	.filter(dirent => dirent.isDirectory())
	.map(dirent => dirent.name);
for(dir in dirlist){
	let files = fs.readdirSync(`./commands/${dirlist[dir]}/`).filter(file=>file.endsWith(".js"));
	for(file in files){
		let command = require(`./commands/${dirlist[dir]}/${files[file]}`);
		client.commands.set(command.name,command);
	}
}

client.once('ready', () => {
	client.shard.fetchClientValues("users.cache").then(res=>{
		client.user.setActivity(`${res[0].filter(user => !user.bot).length} users`, { type: 'WATCHING' });
	})
	console.log((`Mee6 overrated, dyno outdated, ${client.user.username}™ activated`).rainbow);
});

client.on("message",message=>{
	if(message.author.bot) return;
	if(message.guild&&channels.includes(message.channel.id)&&message.member.roles.cache.find(role=>role.name=="Hypixel Verified")==undefined){
		console.log(message.cleanContent);
		verify(message.cleanContent,message);
	}
	try {
		client.commands.get(message.cleanContent).execute(message);
	} catch (error) {
		message.channel.send("This command doesn't exist or something else went wrong!");
		console.error(error.message);
	}

})

client.login(process.env.TOKEN);