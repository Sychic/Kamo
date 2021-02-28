require('dotenv').config({
	path: __dirname + '/.env'
})
const fs = require('fs');
const Discord = require('discord.js');
global.colors = require("colors");
const verify = require('./handlers/verify.js');
const join = require('./handlers/join.js');
const rolesync = require('./handlers/rolesync.js');

const channels = JSON.parse(process.env.CHANNEL);

const client = new Discord.Client({
	ws: {
		intents: ['GUILDS', 'GUILD_MESSAGES', 'GUILD_MEMBERS']
	}
});

client.commands = new Discord.Collection();

let dirlist = fs.readdirSync('./commands', {
		withFileTypes: true
	})
	.filter(dirent => dirent.isDirectory())
	.map(dirent => dirent.name);
for (dir in dirlist) {
	let files = fs.readdirSync(`./commands/${dirlist[dir]}/`).filter(file => file.endsWith(".js"));
	for (file in files) {
		let command = require(`./commands/${dirlist[dir]}/${files[file]}`);
		client.commands.set(command.name, command);
	}
}

client.once('ready', () => {
	client.user.setActivity(`dog`, {
		type: 'WATCHING'
	});
	console.log((`Mee6 overrated, dyno outdated, ${client.user.username}™ activated`).rainbow);
});

client.on("guildMemberAdd", member => {
	join(member);
});

client.on("message", message => {
	if (message.author.bot) return;
	if (message.guild && channels.includes(message.channel.id) /*will have to be replaced by per guild setting*/ && message.member.roles.cache.find(role => role.name == "Hypixel Verified") == undefined) {
		console.log(message.cleanContent);
		verify(message.cleanContent, message);
	}
	if (!message.content.startsWith(",")) return; //default prefix, will be replaced with per guild prefix
	let first = message.content.slice(1, (message.content.includes(" ")) ? message.content.indexOf(" ") : message.content.length);
	let command = client.commands.get(first) || client.commands.find(cmd => cmd.aliases && cmd.aliases.includes(first));
	if (!(command.guildOnly && message.guild)) return;
	try {
		command.execute(message);
	} catch (error) {
		message.channel.send("This command doesn't exist or something else went wrong!");
		console.error(error.message);
	}

});

client.login(process.env.TOKEN);