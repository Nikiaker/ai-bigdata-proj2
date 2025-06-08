create table movie_alerts ( 
    film_id int, 
    windowStart varchar(200),
    windowEnd varchar(200), 
    title varchar(200), 
    count integer,
    avg integer
);

create table etl_monthly_stats ( 
    film_id int,
    title varchar(200),
    count integer,
    sum integer,
    uniques integer
);