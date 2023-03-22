

$local.fred = [ 1 ];
$local.whileDo(
    () -> fred[ 0 ] < 10,
    () -> ( fred[ 0 ] = fred[ 0 ] + 1 ),
    10,
    ( e ) -> c:raise( e )
);



$local.bloggs = 1;
$local.whileDo(
    () -> bloggs < 10,
    () -> ( $local.bloggs = bloggs + 1 ),
    10,
    ( e ) -> c:raise( e )
);
