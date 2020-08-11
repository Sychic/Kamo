const axios = require('axios');
const mongo = require('./mongo.js');
const Discord = require('discord.js');

module.exports = function(username,message){
    //eventually add a regex username checker, currently only removes whitespaces and checks length
    username = username.replace(/\s/g, '');
    if(username.length>16) return message.channel.send("This isn't a valid username!");
    //gets minecraft uuid from mojang
    axios.request({'url':'https://api.mojang.com/users/profiles/minecraft/'+username})
        .then(r=>{
            //fetch hypixel data to see linked discord account
            axios.request({'url':`https://api.hypixel.net/player?key=${process.env.APIKEY}&uuid=${r.data.id}`})
                .then(res=>{
                    let media = (res.data.player.socialMedia==undefined||res.data.player.socialMedia.links==undefined) ? {"DISCORD" : "not linked"} : res.data.player.socialMedia.links;
                    if(media.DISCORD=="not linked"){
                        try {
                            message.channel.send(`<@${message.author.id}>, there is no discord linked to that account!, link your discord by following this educational gif`);
                            message.channel.send(`https://gfycat.com/dentaltemptingleonberger`);
                        } catch (error) {
                            console.error(`unable to send no linked media message`);
                        }
                    }else if(media.DISCORD==message.author.tag){
                        //query doc and check if it exists. If yes, then update it to match new uuid or create a new one if the doc doesn't exist
                        mongo.db('Users').collection('Users').findOne({Discord:message.author.id})
                            .then(doc=>{
                                if(doc==null){
                                    mongo.db('Users').collection('Users').insertOne({"Discord":message.author.id,"uuid":r.data.id});
                                }else{
                                    mongo.db('Users').collection('Users').updateOne({Discord:message.author.id},{$set:{"Discord":message.author.id,"uuid":r.data.id}});
                                }
                            })
                        //looks for Hypixel verified role. If doesn't exist, create one and add, otherwise add existing role
                        try {
                            let role = message.guild.roles.cache.find(role=>role.name=="Hypixel Verified");
                            if(role==undefined){
                                message.guild.roles.create({
                                    data:{
                                        name: "Hypixel Verified",
                                        color: "#77b6b6",
                                    },
                                    reason: "Create role for verified users",
                                })
                                .then(newrole=>{
                                    console.log(`Created Verified role for the ${message.guild.name}(${message.guild.id}) server!`)
                                    try {
                                        message.member.roles.add(newrole,"Verification")
                                        .then(()=>{
                                            message.member.setNickname(res.data.player.displayname,"Verification").then(()=>{
                                                let embed = new Discord.MessageEmbed()
                                                    .setDescription(`<a:verified:741883408728457257> You're all good to go!`)
                                                    .setColor("#1da1f2");
                                                message.channel.send({embed});
                                            });
                                        });
                                    } catch (error) {
                                        message.channel.send(`Unable to add role!`);
                                    }
                                })
                            }else{
                                try {
                                    message.member.roles.add(role,"Verification")
                                    .then(()=>{
                                        message.member.setNickname(res.data.player.displayname,"Verification").then(()=>{
                                            let embed = new Discord.MessageEmbed()
                                                .setDescription(`<a:verified:741883408728457257> You're all good to go!`)
                                                .setColor("#1da1f2");
                                            message.channel.send({embed});
                                        });
                                    })
                                } catch (error) {
                                    message.channel.send(`Unable to add role!`);
                                }
                                
                            }
                        } catch (error) {
                            console.error(`channel not found`);
                            console.error(error);
                        }
                    }else{
                        try {
                            let embed = new Discord.MessageEmbed()
                                .setDescription(`<:error:741896426052648981> The discord linked to that account doesn't match the linked discord!`)
                                .setColor("#ff0033");
                            message.channel.send({embed});
                        } catch (error) {
                            console.error(`Unable to send fail message`);
                        }
                    }
                })
                .catch(e=>{
                    try {
                        let embed = new Discord.MessageEmbed()
                            .setDescription(`<:error:741896426052648981> ${username} has never logged into hypixel!`)
                            .setColor("#ff0033");
                        message.channel.send({embed});
                    } catch (error) {
                        console.error(`unable to send no hypixel login error`);
                    }
                    console.error(`hypixel fetch error`);
                    console.error(e.message);
                })
        })
        .catch(e=>{
            console.error(`uuid fetch error`);
        })
}