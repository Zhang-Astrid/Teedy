-- DBUPDATE-032-0.SQL

-- Create user registration request table
create table T_USER_REGISTRATION_REQUEST (
                                             URR_ID_C varchar(36) primary key,
                                             URR_USERNAME_C varchar(50) not null,
                                             URR_PASSWORD_C varchar(100) not null,
                                             URR_EMAIL_C varchar(100),
                                             URR_CREATEDATE_D timestamp not null,
                                             URR_STATUS_C varchar(10) not null
);

-- Update the database version
update T_CONFIG set CFG_VALUE_C = '32' where CFG_ID_C = 'DB_VERSION';