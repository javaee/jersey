/**
 * Created by mattyu on 2/23/16.
 */

angular.module('jerseytest', [])
.controller('jerseyctrl', ['$scope', '$window', function($scope, $window) {

    $scope.authorize = function() {
        $window.location.href = "http://mattys-macbook-air.local:9999/v1/oauth/auth";
    }

}]);
