create schema chatpro;

create table aprovedsessions (numofses bigserial,login varchar(20), timestampforsess bigint not null );
create table users ( login varchar(20) not null unique, pass varchar(20), code int);
create table sessionsstory (numofstory bigserial,login varchar(20) not null ,sessionid int ,messages varchar(100),timeincome bigint);

create table illigalattempt(numofillegal bigserial,login varchar(20) not null ,pas  varchar(20),mes varchar(100),ses int,timeoftheattempt  bigint,ipadressofattempt  varchar(100));