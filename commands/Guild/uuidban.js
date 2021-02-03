const ban = require('../../handlers/ban.js');

module.exports = {
    name: "uuidban",
    aliases: ["uban"],
    guildOnly: true,
    execute (message) {
        let targetMember = message.mentions.users.first();
        if(!targetMember) return message.reply('you need to tag a user!');
        message.reply(targetMember);
            ban(message, targetMember);
            // message goes below!
            // message.channel.send(`<@${targetMember.user.id}> you just got a hug  https://tenor.com/view/anime-cuddle-cute-gif-12668750`);
        

    }
}
