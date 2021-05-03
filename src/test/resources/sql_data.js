const data = [
    testData(
        'x',
        'INSERT INTO [dbo].[abc] ([x] , [y] , [z]) VALUES (1, 2, 3)'
        ,
        `
            |UPDATE dbo.abc SET y =  2, z =  3  WHERE x = 1
            |IF @@ROWCOUNT=0
            |\tINSERT INTO dbo.abc (x, y, z) VALUES (1,  2,  3);
        `,
        'Sql server 中括號寫法'
    ),


    testData(
        'x',
        'insert into dbo.abc (x , y , z) values (1, 2, 3)'
        ,
        `
            |UPDATE dbo.abc SET y =  2, z =  3  WHERE x = 1
            |IF @@ROWCOUNT=0
            |\tINSERT INTO dbo.abc (x, y, z) VALUES (1,  2,  3);
        `,
        '小寫的insert into與values'
    ),


    testData(
        'x',
        'INSERT dbo.abc (x , y , z) VALUES (1, 2, 3)'
        ,
        `
            |UPDATE dbo.abc SET y =  2, z =  3  WHERE x = 1
            |IF @@ROWCOUNT=0
            |\tINSERT INTO dbo.abc (x, y, z) VALUES (1,  2,  3);
        `,
        '有insert沒有into'
    ),


    testData(
        'x',
        `INSERT INTO dbo.abc (x , y , z) VALUES ('a'',b,\nc,', 2, 3)`
        ,
        `
            |UPDATE dbo.abc SET y =  2, z =  3  WHERE x = 'a'',b,\nc,'
            |IF @@ROWCOUNT=0
            |\tINSERT INTO dbo.abc (x, y, z) VALUES ('a'',b,\nc,',  2,  3);
        `,
        'value含有,\'\\n'
    ),


    testData(
        'x',
        'INSERT INTO dbo.abc (x , y , z) VALUES (function(1), 2, 3)'
        ,
        `
            |UPDATE dbo.abc SET y =  2, z =  3  WHERE x = function(1)
            |IF @@ROWCOUNT=0
            |\tINSERT INTO dbo.abc (x, y, z) VALUES (function(1),  2,  3);
        `,
        'value是函數'
    ),


    testData(
        'x',
        'INSERT INTO dbo.abc (x , y , z) VALUES (1, function(func(\'a\'\',b,\nc,\')), 3)'
        ,
        `
            |UPDATE dbo.abc SET y =  function(func('a'',b,\nc,')), z =  3  WHERE x = 1
            |IF @@ROWCOUNT=0
            |\tINSERT INTO dbo.abc (x, y, z) VALUES (1,  function(func('a'',b,\nc,')),  3);
        `,
        'value是函數且含有敏感字符'
    ),


    testData(
        'x',
        `
            |INSERT dbo.abc (x , y , z) VALUES (1, 2, 3)
            |GO
            |--comment
            |delete from dbo.abc where 1 = 1
            |INSERT dbo.abc (x , y , z) VALUES (4, 5, 6)
        `,
        `
            |UPDATE dbo.abc SET y =  2, z =  3  WHERE x = 1
            |IF @@ROWCOUNT=0
            |\tINSERT INTO dbo.abc (x, y, z) VALUES (1,  2,  3);
            |GO
            |delete from dbo.abc where 1 = 1
            |UPDATE dbo.abc SET y =  5, z =  6  WHERE x = 4
            |IF @@ROWCOUNT=0
            |\tINSERT INTO dbo.abc (x, y, z) VALUES (4,  5,  6);
        `,
        'insert語句之間含有其他語句'
    ),
];

function testData(pk, question, answer, desc) {
    return {p: pk, q: getRealStr(question), a: getRealStr(answer), d: desc};
}

function getRealStr(str) {
    return str.replace(/\s+\|/g, '\n').split('\n').filter(l => l.trim() !== '').join('\n');
}

console.log(JSON.stringify({data}));
