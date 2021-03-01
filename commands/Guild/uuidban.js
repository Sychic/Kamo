const ban = require('../../handlers/ban.js');

module.exports = {
    name: "uuidban",
    aliases: ["uban"],
    guildOnly: true,
    execute(message) {
        let targetMember = message.mentions.users.first();
        if (!targetMember) return message.reply('you need to tag a user!');
        ban(message, targetMember);
    }
}