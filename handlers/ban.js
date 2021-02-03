const mongo = require('../mongo.js');
const Discord = require('discord.js');

module.exports = function(message, user) {
    let userDB = mongo.db(`Users`).collection(`Users`);
    let banDB = mongo.db(`Users`).collection(`Bans`);
    userDB.findOne({Discord:user.id})
        .then(userDoc => {
            if (userDoc==null) {
                return message.reply(`<@${user.id}> has not been verified.`);
            } else {
                banDB.findOne({Discord:user.id})
                    .then(banDoc => {
                        if (banDoc==null) {
                            banDB.insertOne(userDoc)
                                .then(() => {
                                    return message.reply(`<@${user.id}> has been uuid banned (${userDoc.uuid})`);
                                })
                        } else {
                            return message.reply(`<@${user.id}> has already been uuid banned (${userDoc.uuid})`);
                        }
                    })
            }
        })
}