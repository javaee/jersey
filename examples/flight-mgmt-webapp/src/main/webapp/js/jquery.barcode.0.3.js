/*
 * jQuery Barcode plugin 0.3
 *
 * http://www.pasella.it/projects/jQuery/barcode
 * based on code from Kris Bailey [http://www.krisbailey.com/creating-code-39-barcodes-in-pure-javascript]
 *
 * Copyright (c) 2009 Antonello Pasella antonello.pasella@gmail.com
 *
 * Dual licensed under the MIT and GPL licenses:
 *   http://www.opensource.org/licenses/mit-license.php
 *   http://www.gnu.org/licenses/gpl.html
 */


;(function($) {

	$.barcode = {};
	$.barcode.defaults = {thinBarWidth : 1, height: '100%', code: 'code39'};

	$.fn.extend({
		barcode : 		function(settings){
				settings				= $.extend({}, $.barcode.defaults, settings);
				settings.barWidthRatio	= 3;
				settings.thin			= settings.thinBarWidth;
				settings.thick			= settings.thinBarWidth * settings.barWidthRatio;
				return $.barcode._barcodeBMP(settings, this);
			}
	});

	$.barcode.palette = [ [0, 0 ,0] , [255, 255, 255] ];
	$.barcode.codes	= {};
	$.barcode.codes.code39 = {};
	$.barcode.codes.code39.plain = {};
	$.barcode.codes.code39.plain['0'] = 'bwbWBwBwb';
	$.barcode.codes.code39.plain['1'] = 'BwbWbwbwB';
	$.barcode.codes.code39.plain['2'] = 'bwBWbwbwB';
	$.barcode.codes.code39.plain['3'] = 'BwBWbwbwb';
	$.barcode.codes.code39.plain['4'] = 'bwbWBwbwB';
	$.barcode.codes.code39.plain['5'] = 'BwbWBwbwb';
	$.barcode.codes.code39.plain['6'] = 'bwBWBwbwb';
	$.barcode.codes.code39.plain['7'] = 'bwbWbwBwB';
	$.barcode.codes.code39.plain['8'] = 'BwbWbwBwb';
	$.barcode.codes.code39.plain['9'] = 'bwBWbwBwb';
	$.barcode.codes.code39.plain['A'] = 'BwbwbWbwB';
	$.barcode.codes.code39.plain['B'] = 'bwBwbWbwB';
	$.barcode.codes.code39.plain['C'] = 'BwBwbWbwb';
	$.barcode.codes.code39.plain['D'] = 'bwbwBWbwB';
	$.barcode.codes.code39.plain['E'] = 'BwbwBWbwb';
	$.barcode.codes.code39.plain['F'] = 'bwBwBWbwb';
	$.barcode.codes.code39.plain['G'] = 'bwbwbWBwB';
	$.barcode.codes.code39.plain['H'] = 'BwbwbWBwb';
	$.barcode.codes.code39.plain['I'] = 'bwBwbWBwb';
	$.barcode.codes.code39.plain['J'] = 'bwbwBWBwb';
	$.barcode.codes.code39.plain['K'] = 'BwbwbwbWB';
	$.barcode.codes.code39.plain['L'] = 'bwBwbwbWB';
	$.barcode.codes.code39.plain['M'] = 'BwBwbwbWb';
	$.barcode.codes.code39.plain['N'] = 'bwbwBwbWB';
	$.barcode.codes.code39.plain['O'] = 'BwbwBwbWb';
	$.barcode.codes.code39.plain['P'] = 'bwBwBwbWb';
	$.barcode.codes.code39.plain['Q'] = 'bwbwbwBWB';
	$.barcode.codes.code39.plain['R'] = 'BwbwbwBWb';
	$.barcode.codes.code39.plain['S'] = 'bwBwbwBWb';
	$.barcode.codes.code39.plain['T'] = 'bwbwBwBWb';
	$.barcode.codes.code39.plain['U'] = 'BWbwbwbwB';
	$.barcode.codes.code39.plain['V'] = 'bWBwbwbwB';
	$.barcode.codes.code39.plain['W'] = 'BWBwbwbwb';
	$.barcode.codes.code39.plain['X'] = 'bWbwBwbwB';
	$.barcode.codes.code39.plain['Y'] = 'BWbwBwbwb';
	$.barcode.codes.code39.plain['Z'] = 'bWBwBwbwb';
	$.barcode.codes.code39.plain['-'] = 'bWbwbwBwB';
	$.barcode.codes.code39.plain['.'] = 'BWbwbwBwb';
	$.barcode.codes.code39.plain[' '] = 'bWBwbwBwb';
	$.barcode.codes.code39.plain['*'] = 'bWbwBwBwb';
	$.barcode.codes.code39.plain['$'] = 'bWbWbWbwb';
	$.barcode.codes.code39.plain['/'] = 'bWbWbwbWb';
	$.barcode.codes.code39.plain['+'] = 'bWbwbWbWb';
	$.barcode.codes.code39.plain['%'] = 'bwbWbWbWb';

	$.barcode.codes.I25 = {};
	$.barcode.codes.I25.plain = {};
	$.barcode.codes.I25.plain['START']  = 'wwwwwwwwwwbwbw';
	$.barcode.codes.I25.plain['END']  = 'Bwbwwwwwwwwww';
	$.barcode.codes.I25.plain['0'] = 'bbBBb';
	$.barcode.codes.I25.plain['1'] = 'BbbbB';
	$.barcode.codes.I25.plain['2'] = 'bBbbB';
	$.barcode.codes.I25.plain['3'] = 'BBbbb';
	$.barcode.codes.I25.plain['4'] = 'bbBbB';
	$.barcode.codes.I25.plain['5'] = 'BbBbb';
	$.barcode.codes.I25.plain['6'] = 'bBBbb';
	$.barcode.codes.I25.plain['7'] = 'bbbBB';
	$.barcode.codes.I25.plain['8'] = 'BbbBb';
	$.barcode.codes.I25.plain['9'] = 'bBbBb';

	/* Cache BMP translation codes*/
	$.barcode.codes.code39.BMP = {};
	for (var x in $.barcode.codes.code39.plain){
		var tcodes = $.barcode.codes.code39.plain[x];
		$.barcode.codes.code39.BMP[x] = '';
		for (var xi=0; xi<9; xi++)
			switch (tcodes.charAt(xi)){
				case 'B':
					$.barcode.codes.code39.BMP[x] += '\x00\x00\x00\x00\x00\x00\x00\x00\x00';
					break;
				case 'b':
					$.barcode.codes.code39.BMP[x] += '\x00\x00\x00';
					break;
				case 'W':
					$.barcode.codes.code39.BMP[x] += '\x01\x01\x01\x01\x01\x01\x01\x01\x01';
					break;
				case 'w':
					$.barcode.codes.code39.BMP[x] += '\x01\x01\x01';
					break;
			}
		$.barcode.codes.code39.BMP[x] += '\x01\x01\x01';						
	};

	$.barcode._barcodeBMP =	function(settings, jQ) {
		return $(jQ).each(function(item, index){
				var encoded = '';
				switch(settings.code){
					case 'code39' :
						var code = '*' + $.trim($(this).html()) + '*';
						for(var i = 0; i < code.length; i++)
							encoded += $.barcode.codes.code39.BMP[code.charAt(i)];
						break;
					case 'I25' :
						var code = $.trim($(this).html());
					    encoded = 	$.barcode.codes.I25.plain['START'];

						if(code.length % 2 == 1)
							code = '0' + code;
						
						for(var i = 0; i < code.length; i+=2){
							var code1 = $.barcode.codes.I25.plain[code.charAt(i)];
							var code2 = $.barcode.codes.I25.plain[code.charAt(i+1)].replace(/b/g,"w").replace(/B/g, "W");
							for(var i2 = 0; i2 < 5; i2++){
								encoded += code1.charAt(i2) + code2.charAt(i2);
							}
						}
						encoded += 	$.barcode.codes.I25.plain['END'];
						encoded = encoded.replace(/w/g, '\x01\x01\x01');
						encoded = encoded.replace(/W/g, '\x01\x01\x01\x01\x01\x01\x01\x01\x01');
						encoded = encoded.replace(/b/g, '\x00\x00\x00');
						encoded = encoded.replace(/B/g, '\x00\x00\x00\x00\x00\x00\x00\x00\x00');
						break;
					default : 
						alert("Code" + settings.code + ' not implemented');
						return;
				};
				var img = $("<img />").width("100%").height("100%").attr("src", $.barcode._createBmp([encoded], $.barcode.palette));
				$(this).html("").append(img);
			});
	};
	
	$.barcode._encode64 = function(input) {
		var output = '';
		var i = 0;
		do {
			var chr1 = input.charCodeAt(i++);
			var chr2 = input.charCodeAt(i++);
			var chr3 = input.charCodeAt(i++);

			var enc1 = chr1 >> 2;
			var enc2 = ((chr1 & 3) << 4) | (chr2 >> 4);
			var enc3 = ((chr2 & 15) << 2) | (chr3 >> 6);
			var enc4 = chr3 & 63;

			if (isNaN(chr2))		enc3 = enc4 = 64;
			else if (isNaN(chr3))   enc4 = 64;
			
			output = output + $.barcode._encode64.keyStr.charAt(enc1) +
								$.barcode._encode64.keyStr.charAt(enc2) + 
								$.barcode._encode64.keyStr.charAt(enc3) +
								$.barcode._encode64.keyStr.charAt(enc4);
		} while (i < input.length);
		return output;
	};	
	
	$.barcode._encode64.keyStr = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=';
	
	$.barcode._multiByteEncode = function(number, bytes) {
		if (number < 0 || bytes < 0) {
			throw('Negative numbers not allowed.');
		}
		var oldbase = 1;
		var string = '';
		for (var x = 0; x < bytes; x++) {
			if (number == 0) {
				CharCode = 0;
			} else {
				var base = oldbase * 256;
				var CharCode = number % base;
				number = number - CharCode;
				CharCode = CharCode / oldbase;
				oldbase = base;
			}
			string += String.fromCharCode(CharCode);
		}
		if (number != 0)
			throw('Overflow, number too big for string length');
		return string;
	};
	
	if ('btoa' in window && typeof window.btoa == 'function' && window.btoa('hello') == 'aGVsbG8=') {
		$.barcode._encode64 = function(input) {
			return window.btoa(input);
		}
	};
	
	$.barcode._createBmp = function(grid, palette) {
		// xxxx and yyyy are placeholders for offsets (computed later).
		var bitmapFileHeader = 'BMxxxx\0\0\0\0yyyy';

		// Assemble the info header.
		var height = grid.length;
		var width = height && grid[0].length;
		var biHeight = this._multiByteEncode(height, 4);
		var biWidth = this._multiByteEncode(width, 4);
		var bfOffBits = this._multiByteEncode(40, 4);
		var bitCount = 8;

		var biBitCount = this._multiByteEncode(bitCount, 2);
		var bitmapInfoHeader = bfOffBits + biWidth + biHeight + '\x01\0' +
		biBitCount + '\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0';

		if (bitCount != 24) {
			var palette_str = String(palette);
			var rgbQuad = [];
			var r = 0;
			var g = 0;
			var b = 0;
			for (var x = 0; x < 256; x++) {
				if (x < palette.length) {
					r = palette[x][0];
					g = palette[x][1];
					b = palette[x][2];
				}
				rgbQuad[x] = String.fromCharCode(b, g, r, 0);
			}
			rgbQuad = rgbQuad.join('');
		}

		var padding;
		if      (width % 4 == 1) padding = '\0\0\0';
		else if (width % 4 == 2) padding = '\0\0';
		else if (width % 4 == 3) padding = '\0';
		else 					 padding = '';

		var data = [];
		for (var y = 0; y < height; y++) 
			data[y] = grid[height - y - 1] + padding;

		var bitmap = bitmapFileHeader + bitmapInfoHeader + rgbQuad + data.join('');
		bitmap = bitmap.replace(/yyyy/, this._multiByteEncode(bitmapFileHeader.length + bitmapInfoHeader.length + rgbQuad.length, 4));
		bitmap = bitmap.replace(/xxxx/, this._multiByteEncode(bitmap.length, 4));
		return 'data:image/bmp;base64,' + this._encode64(bitmap);
	};
})(jQuery);


/**

A version with DIVs for IE < 8 ...


				//For Ie<8 fallback to DIV version
				if($.browser.msie && parseInt($.browser.version.charAt(0)) < 8)
					return $.barcode._barcodeDIV(settings, this);
				else



	$.barcode._barcodeDIV =	function(settings, jQ) {
		settings.thinWhiteBar   = "<div style='float:left;background-color:#FFF;width:"+settings.thin+"px;height:"+settings.height+"'></div>";
		settings.thinBlackBar   = "<div style='float:left;background-color:#000;width:"+settings.thin+"px;height:"+settings.height+"'></div>";
		settings.thickWhiteBar  = "<div style='float:left;background-color:#FFF;width:"+settings.thick+"px;height:"+settings.height+"'></div>";
		settings.thickBlackBar  = "<div style='float:left;background-color:#000;width:"+settings.thick+"px;height:"+settings.height+"'></div>";
		var codesHTML = {};
		for (var x in $.barcode.codes.code39){
			var htmlRES = '';
			var tcodes = $.barcode.codes.code39[x];
			for (var xi=0; xi<9; xi++){
				switch (tcodes.charAt(xi)){
					case 'w':
						htmlRES += settings.thinWhiteBar;
					break;
					case 'b':
						htmlRES += settings.thinBlackBar;
					break;
					case 'W':
						htmlRES += settings.thickWhiteBar;
					break;
					case 'B':
						htmlRES += settings.thickBlackBar;
					break;
				}
			}
			htmlRES += settings.thinWhiteBar;
			codesHTML[x] = htmlRES;
		};
		
		return $(jQ).each(function(index, item) {
			var code = '*'+$.trim($(item).attr("alt")).toUpperCase() + '*';
			//alert(code);
			var htmlout = "";
			for (var i=0; i<code.length; i++){
				tcodes = $.barcode.codes.code39[code.charAt(i)];
				if (typeof tcodes!='undefined') 
					htmlout += codesHTML[code.charAt(i)];
			}
			$(item).wrap("<div></div>");
			$(item).parent().css("width", $(item).width());
			$(item).parent().css("height", $(item).height());
			$(item).parent().html(htmlout);
		});
	};


*/