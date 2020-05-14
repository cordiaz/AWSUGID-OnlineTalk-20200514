$(function () {
    var topicMessages;
    var topicMetrics;

    function disable() {
        $('#topicSelector, #refreshButton, #emailInput, #subscribeButton').prop('disabled', true);
    }

    function enable() {
        $('#topicSelector, #refreshButton, #emailInput, #subscribeButton').prop('disabled', false);
    }

    function refreshTopics() {
        disable();

        $.get({
            url: "https://0734o0j5yg.execute-api.ap-southeast-1.amazonaws.com/Prod/topics",
            beforeSend: function(jqXHR){
                jqXHR.setRequestHeader('X-API-Key', 'mEPxDkw3Kgasg0n27n3YR7fzdst0lAT8CBAQANSg');
            },
            success: function(data) {
                enable();
                if (data && data.Items.length) {
                    $('#topicSelector').empty().append(
                        data.Items.map(function (i) {
                            return '<option value="' + i.arn + '">' + i.name + '</option>'
                        }).join(""));
                } else {
                    $('#topicSelector').empty();
                }
                    
            },
            error: function() {
                $('#topicSelector').empty();
                alert('Gagal! Mungkin sesinya dah berakhir?');
            }
        });
    }

    $('#refreshButton').click(refreshTopics);

    $('#subscribeButton').click(function() {
        var topics = $('#topicSelector').val();
        var email = $('#emailInput').val();
        if (!(topics && topics.length) && !(email && email.length)) {
            var errorMessages = [
                "Tolong pilih topik yah. Oh, emailnya sekalian.",
                "Aduh, belum dipilih topiknya. Emailnya juga belum.",
                "Topiknya kelupaan. Emailnya perlu juga nih.",
                "Eh, perlu topik dan emailnya mas/mbak.",
                "Mohon maaf, topiknya mau yang mana? Emailnya juga donk."
            ];
            var index = Math.floor(Math.random() * errorMessages.length);
            alert(errorMessages[index]);
            return;
        } else if (!(topics && topics.length)) {
            var errorMessages2 = [
                "Hmm, belum milih topik yah?",
                "Topiknya lupa mas/mbak.",
                "Masih mikir mau topik yang mana?",
                "Mas/mbak, mau pesen topik yang mana?",
                "Duh, keknya belum nih topiknya."
            ];
            var index2 = Math.floor(Math.random() * errorMessages2.length);
            alert(errorMessages2[index2]);
            return;
        } else if (!(email && email.length)) {
            var errorMessages3 = [
                "Mas/mbak, emailnya donk. Buat testing aja.",
                "Perlu emailnya nih. Janji ga dikirim macem2 deh.",
                "Belum masukin email yah?",
                "Wah, emailnya masih kosong. Tolong diisi yah.",
                "Yah, kelupaan emailnya."
            ];
            var index3 = Math.floor(Math.random() * errorMessages3.length);
            alert(errorMessages3[index3]);
            return;
        }

        var jsonData = JSON.stringify({
            Topics: topics,
            Email: email
        });

        disable();
        $.post({
            beforeSend: function(jqXHR){
                jqXHR.setRequestHeader('X-API-Key', 'mEPxDkw3Kgasg0n27n3YR7fzdst0lAT8CBAQANSg');
            },
            cache: false,
            contentType: "application/json",
            data: jsonData,
            dataType: "json",
            error: function() {
                enable();
                alert("Aduh, gagal nih. Mungkin sesinya dah berakhir?");
            },
            processData: false,
            success: function() {
                $('#topicSelector').val('');
                $('#emailInput').val('');
                enable();
                alert("Sukses! Tolong diconfirm emailnya yah.");
            },
            url: "https://0734o0j5yg.execute-api.ap-southeast-1.amazonaws.com/Prod/subscriptions"
        });
    });

    function disableCheckSubs() {
        $('#checkButton, #emailToCheckInput').prop('disabled', true);
    }

    function enableCheckSubs() {
        $('#checkButton, #emailToCheckInput').prop('disabled', false);
    }

    $('#checkButton').click(function() {
        var emailToCheck = $('#emailToCheckInput').val();

        if (emailToCheck && emailToCheck.length) {
            disableCheckSubs();

            $.post({
                beforeSend: function(jqXHR){
                    jqXHR.setRequestHeader('X-API-Key', 'mEPxDkw3Kgasg0n27n3YR7fzdst0lAT8CBAQANSg');
                },
                cache: false,
                contentType: "application/json",
                data: JSON.stringify({email: emailToCheck}),
                dataType: "json",
                error: function() {
                    enableCheckSubs();
                    alert("Cek email nya gagal. Mungkin sesinya dah berakhir?");
                },
                processData: false,
                success: function(data) {
                    enableCheckSubs();
                    //var topics = JSON.parse(data);
                    if (data && data.length) {
                        $('#topicsResult').val(data.join('\n'));
                    } else {
                        $('#topicsResult').val("[Maaf, ga ada subscription buat emailnya]");
                    }
                    
                },
                url: "https://0734o0j5yg.execute-api.ap-southeast-1.amazonaws.com/Prod/topics"
            })

        } else {
            alert("Mohon email yang mau dicek dimasukin yah.");
        }
    });

    function disableMessages() {
        $('#topicList, #refreshTopicsMessagesButton').prop('disabled', true);
    }

    function enableMessages() {
        $('#topicList, #refreshTopicsMessagesButton').prop('disabled', false);
    }

    function refreshTopicsMessages() {
        disableMessages();

        $.get({
            url: "https://0734o0j5yg.execute-api.ap-southeast-1.amazonaws.com/Prod/topics/messages",
            beforeSend: function (jqXHR) {
                jqXHR.setRequestHeader('X-API-Key', 'mEPxDkw3Kgasg0n27n3YR7fzdst0lAT8CBAQANSg');
            },
            success: function (data) {
                enableMessages();
                topicMessages = JSON.parse(data);
                if (topicMessages && topicMessages.length) {
                    $('#topicList').empty().append(
                        topicMessages.map(function (i) {
                            return '<option value="' + i.id + '">' + i.name + '</option>'
                        }).join("")).change();
                } else {
                    $('#topicList').empty();
                }
                
            },
            error: function () {
                enableMessages();
                $('#topicList').empty();
                alert('Yah, error nih...');
            }
        });
    }

    $('#refreshTopicsMessagesButton').click(refreshTopicsMessages)

    $('#topicList').change(function () {
        if (topicMessages && topicMessages.length) {
            var topic = topicMessages.find(function (t) {
                return t.id === $('#topicList').val();
            });
            $('#messagesResult').empty().append(
                topic.messages.map(function (m) {
                    return '<li class="list-group-item">' + m + '</li>';
                }).join(""));   
            $('#cronInfo').val(topic.cron);
        } else {
            $('#messagesResult').empty();
            $('#cronInfo').val('');
        }
    });

    function disableMetrics() {
        $('#topicMetricList, #refreshTopicsMetricsButton').prop('disabled', true);
    }

    function enableMetrics() {
        $('#topicMetricList, #refreshTopicsMetricsButton').prop('disabled', false);
    }

    function refreshTopicsMetrics() {
        disableMetrics();

        $.get({
            url: "https://0734o0j5yg.execute-api.ap-southeast-1.amazonaws.com/Prod/topics/metrics",
            beforeSend: function (jqXHR) {
                jqXHR.setRequestHeader('X-API-Key', 'mEPxDkw3Kgasg0n27n3YR7fzdst0lAT8CBAQANSg');
            },
            success: function (data) {
                enableMetrics();
                topicMetrics = JSON.parse(data);
                if (topicMetrics && topicMetrics.length) {
                    $('#topicMetricList').empty().append(
                        topicMetrics.map(function (i) {
                            return '<option value="' + i.name + '">' + i.name + '</option>'
                        }).join("")).change();
                } else {
                    $('#topicMetricList').empty();
                }

            },
            error: function () {
                enableMetrics();
                $('#topicMetricList').empty();
                alert('Wah, gagal dapetin metrics-nya');
            }
        });
    }

    $('#topicMetricList').change(function () {
        if (topicMetrics && topicMetrics.length) {
            var topic = topicMetrics.find(function (t) {
                return t.name === $('#topicMetricList').val();
            });

            if (topic && topic.metrics.length) {
                var title = topic.name;
                var data = topic.metrics.map(function (m) {
                    return { x: Date.UTC(m.year, m.month, m.day, m.hour, m.minute), y: m.value };
                });

                Highcharts.chart('topicMetricChart', {
                    title: {
                        text: title
                    },
                    xAxis: {
                        type: 'datetime',
                        dateTimeLabelFormats: {
                            day: '%e/%m/%Y',
                            hour: '%H:%M',
                            minute: '%H:%M'
                        },
                        labels: {
                            rotation: 45
                        },
                        startOnTick: true
                    },
                    yAxis: {
                        title: {
                            text: "Published Count"
                        }
                    },
                    credits: {
                        enabled: false
                    },
                    exporting: {
                        enabled: false
                    },
                    legend: {
                        enabled: false
                    },
                    series: [{
                        data: data,
                        tooltip: {
                            dateTimeLabelFormats: {
                                day: '%e/%m/%Y %H:%M',
                                hour: '%H:%M',
                                minute: '%H:%M'
                            },
                            pointFormat: '<b>{point.y}</b>'
                        },
                        turboThreshold: 1440
                    }],
                    time: {
                        useUTC: false
                    }
                });
            }           

        } else {
            //$('#topicMetricChart').empty();
        }
    });

    $('#refreshTopicsMetricsButton').click(refreshTopicsMetrics);

    //refreshTopics();
    //refreshTopicsMessages();
    //refreshTopicsMetrics();
});