-- Hard cases for migration 003, on top of legacy_rows.sql and staff_rows.sql (after 001 and 002).
-- Ali (1002) was promoted to employee: same username, his own password copy.
INSERT INTO workerinfo (username, password, fullname) VALUES ('1002', 'pass5678', 'Ali Rezaei');
-- Sara (1001) asked for a second account that is still waiting for review.
INSERT INTO userinfo (username,password,fullname,id,birth,gender,marital,national,address,postalcode,phonenumber,email,occupation,employeraddress,annualincome,creationdate,accounttype,accountnumber,cardnumber,cvv,iban,expiredate,deposit,idphoto,statement) VALUES
 (1001,'pass1234','Sara Ahmadi','0011223344','1380/01/01','Female','Single','Iranian','Tabriz','5100000000','09120000001','s@x.ir','Engineer','Tabriz','100','2025-04-01','checking','1000000005','6037990000000005','111','IR000000000000000000000005','2030.09',0,x'00','Pending'),
-- a different person who gave the same phone number as Ali
 (1004,'pass0000','Neda Moradi','0055667788','2001.5.20','Female','Single','Iranian','Ahvaz','6100000000','09120000002','n@x.ir','Nurse','Ahvaz','300','2025-05-01','checking','1000000006','6037990000000006','222','IR000000000000000000000006','2030.11',750000,x'00','Accepted');
-- an old bill that stored the card number, and an old loan with the term bug ("16" means 2 years)
INSERT INTO bills (username,billtype,billid,billamount,whopaid,accountno,payeddate,duedate) VALUES (1001,'Gas Bill',555000,2000000,'1001','6037990000000001',NOW(),'2025.04.30');
INSERT INTO loan (username,loanamount,term,intrest,date,id,employment,collateral,deed,status) VALUES (1004,'7000000.0','16','16%','2025.03.03',x'01',x'02',x'03',x'04','Final step');
-- an old IBAN transfer: the source known only by an IBAN typed without "IR"
INSERT INTO transitions (transitiontype,sourcefullname,sourcecard,sourceaccount,sourceiban,moneyamount,destinationfullname,destinationcard,destinationaccount,destinationiban,destinationbankname,time) VALUES
 ('iBan','1004',NULL,NULL,'000000000000000000000006',50000,'Unknown',NULL,NULL,'990000000000000000000099','Unknown',NOW());
