-- extra rows so every staff screen has something to show
INSERT INTO userinfo (username,password,fullname,id,birth,gender,marital,national,address,postalcode,phonenumber,email,occupation,employeraddress,annualincome,creationdate,accounttype,accountnumber,cardnumber,cvv,iban,expiredate,deposit,idphoto,statement) VALUES
 (1003,'pass9999','Mina Karimi','0099887766','1382/03/03','Female','Single','Iranian','Shiraz','7100000000','09120000003','m@x.ir','Designer','Shiraz','200','2025-03-01','checking','1000000004','6037990000000004','321','IR000000000000000000000004','08/30',0,x'','Pending');
INSERT INTO transitions (transitiontype,sourcefullname,sourcecard,sourceaccount,sourceiban,moneyamount,destinationfullname,destinationcard,destinationaccount,destinationiban,destinationbankname,time) VALUES
 ('Card','Sara Ahmadi','6037990000000001','1000000001','IR000000000000000000000001',250000,'Ali Rezaei','6037990000000002','1000000002','IR000000000000000000000002','WA Bank',NOW());
INSERT INTO bills (username,billtype,billid,billamount,whopaid,accountno,payeddate,duedate) VALUES (1001,'Electricity',778899,120000,'Sara Ahmadi','1000000001',NOW(),'2025-04-01');
INSERT INTO charge (username,accountno,telephoneno,amount,time) VALUES (1002,'1000000002','09121112233',50000,NOW());
INSERT INTO `check` (issuername,checkamount,recipientdate,date,duedate,status) VALUES ('Sara Ahmadi',300000,'Ali Rezaei',NOW(),DATE_ADD(NOW(), INTERVAL 10 DAY),'Pending'),('Ali Rezaei',90000,'Sara Ahmadi',NOW(),DATE_ADD(NOW(), INTERVAL 5 DAY),'accepted');
INSERT INTO deposit (username,type,amount,duration,interest,payment,currency,date) VALUES (1001,'Long term',1000000,'12 months','18%','Monthly','IRR','2025-02-02');
INSERT INTO bossinfo (username,password,fullname) VALUES (1,'boss1234','Maryam Boss');
