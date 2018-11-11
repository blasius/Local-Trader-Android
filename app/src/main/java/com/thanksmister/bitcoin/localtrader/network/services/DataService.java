/*
 * Copyright (c) 2018 ThanksMister LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.thanksmister.bitcoin.localtrader.network.services;

import javax.inject.Singleton;


@Deprecated
@Singleton
public class DataService {

    /*private static final String PREFS_METHODS_EXPIRE_TIME = "pref_methods_expire";
    private static final String PREFS_CURRENCY_EXPIRE_TIME = "pref_currency_expire";
    private static final String PREFS_EXCHANGE_EXPIRE_TIME = "pref_exchange_expire";
    private static final String PREFS_ADVERTISEMENT_EXPIRE_TIME = "pref_ads_expire";
    private static final String PREFS_WALLET_EXPIRE_TIME = "pref_wallet_expire";
    private static final String PREFS_WALLET_BALANCE_EXPIRE_TIME = "pref_wallet_balance_expire";
    private static final String PREFS_CONTACTS_EXPIRE_TIME = "pref_contacts_expire";
    private static final int CHECK_CURRENCY_DATA = 604800000;// // 1 week 604800000
    private static final int CHECK_METHODS_DATA = 604800000;// // 1 week 604800000
    private static final int CHECK_ADVERTISEMENT_DATA = 3600000;// 1 hour
    private static final int CHECK_CONTACTS_DATA = 3 * 60 * 1000;// 5 minutes
    private static final int CHECK_WALLET_DATA = 15 * 60 * 1000;// 15 minutes
    private static final int CHECK_WALLET_BALANCE_DATA = 15 * 60 * 1000;// 15 minutes

    private final LocalBitcoinsService localBitcoins;
    private final SharedPreferences sharedPreferences;
    private final DPreference preference;

    @Inject
    public DataService(BaseApplication baseApplication, DPreference preference, SharedPreferences sharedPreferences, LocalBitcoinsService localBitcoins) {
        this.localBitcoins = localBitcoins;
        this.sharedPreferences = sharedPreferences;
        this.preference = preference;
    }

    public void logout() {
        resetExchangeExpireTime();
        resetAdvertisementsExpireTime();
        resetMethodsExpireTime();
        resetContactsExpireTime();
        resetWalletBalanceExpireTime();
        resetWalletExpireTime();
    }

    public Observable<Authorization> getAuthorization(String code) {
        return localBitcoins.getAuthorization("authorization_code", code, BuildConfig.LBC_KEY, BuildConfig.LBC_SECRET)
                .map(new ResponseToAuthorize());
    }

    private Observable<String> refreshTokens() {
        final String refreshToken = AuthUtils.getRefreshToken(preference, sharedPreferences);
        return localBitcoins.refreshToken("refreshToken", refreshToken, BuildConfig.LBC_KEY, BuildConfig.LBC_SECRET)
                .map(new ResponseToAuthorize())
                .flatMap(new Func1<Authorization, Observable<? extends String>>() {
                    @Override
                    public Observable<? extends String> call(Authorization authorization) {
                        Timber.d("Access token " + authorization.accessToken);
                        Timber.d("Refresh token " + authorization.refreshToken);
                        AuthUtils.setAccessToken(preference, authorization.accessToken);
                        AuthUtils.setRefreshToken(preference, authorization.refreshToken);
                        return Observable.just(authorization.accessToken);
                    }
                });
    }

    public Observable<List<Currency>> getCurrencies() {
        return localBitcoins.getCurrencies()
                .map(new ResponseToCurrencies());
    }

    public Observable<ContactRequest> createContact(final String adId, final TradeType tradeType, final String countryCode,
                                                    final String onlineProvider, final String amount, final String name,
                                                    final String phone, final String email, final String iban, final String bic,
                                                    final String reference, final String message, final String sortCode,
                                                    final String billerCode, final String accountNumber, final String bsb,
                                                    final String ethereumAddress) {

        final String accessToken = AuthUtils.getAccessToken(preference, sharedPreferences);

        return createContactObservable(accessToken, adId, tradeType, countryCode, onlineProvider, amount,
                name, phone, email, iban, bic, reference, message, sortCode, billerCode, accountNumber, bsb, ethereumAddress)
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                    @Override
                    public Observable<? extends Response> call(Throwable throwable) {
                        NetworkException networkException = null;
                        if (throwable instanceof NetworkException) {
                            networkException = (NetworkException) throwable;
                        }
                        if(networkException != null) {
                            if(networkException.getStatus() == DataServiceUtils.STATUS_403) {
                                Timber.d("refreshTokenAndRetry 403");
                                return refreshTokens()
                                        .flatMap(new Func1<String, Observable<? extends Response>>() {
                                            @Override
                                            public Observable<? extends Response> call(String token) {
                                                Timber.d("new token: " + token);
                                                return createContactObservable(token, adId, tradeType, countryCode, onlineProvider, amount,
                                                        name, phone, email, iban, bic, reference, message, sortCode, billerCode, accountNumber, bsb, ethereumAddress);
                                            }
                                        });
                            } else if (networkException.getStatus() == DataServiceUtils.STATUS_400) {
                                Timber.d("refreshTokenAndRetry 400");
                                if (networkException.getCode() == DataServiceUtils.CODE_THREE) {
                                    return refreshTokens()
                                            .flatMap(new Func1<String, Observable<? extends Response>>() {
                                                @Override
                                                public Observable<? extends Response> call(String token) {
                                                    Timber.d("new token: " + token);
                                                    return createContactObservable(token, adId, tradeType, countryCode, onlineProvider, amount,
                                                            name, phone, email, iban, bic, reference, message, sortCode, billerCode, accountNumber, bsb, ethereumAddress);
                                                }
                                            });
                                }
                            }
                            return Observable.error(networkException);
                        }
                        return Observable.error(throwable);
                    }
                })
                .map(new ResponseToContactRequest());
    }


    private Observable<Response> createContactObservable(final String accessToken, final String adId, final TradeType tradeType, final String countryCode,
                                                         final String onlineProvider, final String amount, final String name,
                                                         final String phone, final String email, final String iban, final String bic,
                                                         final String reference, final String message, final String sortCode,
                                                         final String billerCode, final String accountNumber, final String bsb,
                                                         final String ethereumAddress) {

        if (tradeType == TradeType.ONLINE_BUY) {
            switch (onlineProvider) {
                case TradeUtils.INSTANCE.getNATIONAL_BANK():
                    switch (countryCode) {
                        case "UK":
                            return localBitcoins.createContactNational_UK(accessToken, adId, amount, name, sortCode, reference, accountNumber, message);
                        case "AU":
                            return localBitcoins.createContactNational_AU(accessToken, adId, amount, name, bsb, reference, accountNumber, message);
                        case "FI":
                            return localBitcoins.createContactNational_FI(accessToken, adId, amount, name, iban, bic, reference, message);
                        default:
                            return localBitcoins.createContactNational(accessToken, adId, amount, message);
                    }
                case TradeUtils.INSTANCE.getVIPPS():
                case TradeUtils.INSTANCE.getEASYPAISA():
                case TradeUtils.INSTANCE.getHAL_CASH():
                case TradeUtils.INSTANCE.getQIWI():
                case TradeUtils.INSTANCE.getLYDIA():
                case TradeUtils.INSTANCE.getSWISH():
                    return localBitcoins.createContactPhone(accessToken, adId, amount, phone, message);
                case TradeUtils.INSTANCE.getPAYPAL():
                case TradeUtils.INSTANCE.getNETELLER():
                case TradeUtils.INSTANCE.getINTERAC():
                case TradeUtils.INSTANCE.getALIPAY():
                case TradeUtils.INSTANCE.getMOBILEPAY_DANSKE_BANK():
                case TradeUtils.INSTANCE.getMOBILEPAY_DANSKE_BANK_DK():
                case TradeUtils.INSTANCE.getMOBILEPAY_DANSKE_BANK_NO():
                    return localBitcoins.createContactEmail(accessToken, adId, amount, email, message);
                case TradeUtils.INSTANCE.getSEPA():
                    return localBitcoins.createContactSepa(accessToken, adId, amount, name, iban, bic, reference, message);
                case TradeUtils.INSTANCE.getALTCOIN_ETH():
                    return localBitcoins.createContactEthereumAddress(accessToken, adId, amount, ethereumAddress, message);
                case TradeUtils.INSTANCE.getBPAY():
                    return localBitcoins.createContactBPay(accessToken, adId, amount, billerCode, reference, message);
            }
        } else if (tradeType == TradeType.ONLINE_SELL) {
            switch (onlineProvider) {
                case TradeUtils.INSTANCE.getQIWI():
                case TradeUtils.INSTANCE.getSWISH():
                case TradeUtils.INSTANCE.getMOBILEPAY_DANSKE_BANK():
                case TradeUtils.INSTANCE.getMOBILEPAY_DANSKE_BANK_DK():
                case TradeUtils.INSTANCE.getMOBILEPAY_DANSKE_BANK_NO():
                    return localBitcoins.createContactPhone(accessToken, adId, amount, phone, message);

            }
        }

        return localBitcoins.createContact(accessToken, adId, amount, message);
    }

    public Observable<Boolean> sendPinCodeMoney(final String pinCode, final String address, final String amount) {
        final String accessToken = AuthUtils.getAccessToken(preference, sharedPreferences);
        return sendPinCodeMoneyObservable(accessToken, pinCode, address, amount)
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                    @Override
                    public Observable<? extends Response> call(Throwable throwable) {
                        NetworkException networkException = null;
                        if (throwable instanceof NetworkException) {
                            networkException = (NetworkException) throwable;
                        }
                        if(networkException != null) {
                            if(networkException.getStatus() == DataServiceUtils.STATUS_403) {
                                Timber.d("refreshTokenAndRetry 403");
                                return refreshTokens()
                                        .flatMap(new Func1<String, Observable<? extends Response>>() {
                                            @Override
                                            public Observable<? extends Response> call(String token) {
                                                Timber.d("new token: " + token);
                                                return sendPinCodeMoneyObservable(token, pinCode, address, amount);
                                            }
                                        });
                            } else if (networkException.getStatus() == DataServiceUtils.STATUS_400) {
                                Timber.d("refreshTokenAndRetry 400");
                                if (networkException.getCode() == DataServiceUtils.CODE_THREE) {
                                    return refreshTokens()
                                            .flatMap(new Func1<String, Observable<? extends Response>>() {
                                                @Override
                                                public Observable<? extends Response> call(String token) {
                                                    Timber.d("new token: " + token);
                                                    return sendPinCodeMoneyObservable(token, pinCode, address, amount);
                                                }
                                            });
                                }
                            }
                            return Observable.error(networkException);
                        }
                        return Observable.error(throwable);
                    }
                })
                .map(new ResponseToJSONObject())
                .flatMap(new Func1<JSONObject, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> call(JSONObject jsonObject) {
                        if (Parser.containsError(jsonObject)) {
                            RetroError retroError = Parser.parseError(jsonObject);
                            throw new Error(retroError);
                        }
                        return Observable.just(true);
                    }
                });
    }

    private Observable<Response> sendPinCodeMoneyObservable(String accessToken, final String pinCode, final String address, final String amount) {
        return localBitcoins.walletSendPin(accessToken, pinCode, address, amount);
    }

    public Observable<Wallet> getWalletBalance() {
        if (!needToRefreshWalletBalance()) {
            return Observable.just(null);
        }

        final String accessToken = AuthUtils.getAccessToken(preference, sharedPreferences);
        return getWalletBalanceObservable(accessToken)
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                    @Override
                    public Observable<? extends Response> call(Throwable throwable) {
                        NetworkException networkException = null;
                        if (throwable instanceof NetworkException) {
                            networkException = (NetworkException) throwable;
                        }
                        if(networkException != null) {
                            if(networkException.getStatus() == DataServiceUtils.STATUS_403) {
                                Timber.d("refreshTokenAndRetry 403");
                                return refreshTokens()
                                        .flatMap(new Func1<String, Observable<? extends Response>>() {
                                            @Override
                                            public Observable<? extends Response> call(String token) {
                                                Timber.d("new token: " + token);
                                                return getWalletBalanceObservable(token);
                                            }
                                        });
                            } else if (networkException.getStatus() == DataServiceUtils.STATUS_400) {
                                Timber.d("refreshTokenAndRetry 400");
                                if (networkException.getCode() == DataServiceUtils.CODE_THREE) {
                                    return refreshTokens()
                                            .flatMap(new Func1<String, Observable<? extends Response>>() {
                                                @Override
                                                public Observable<? extends Response> call(String token) {
                                                    Timber.d("new token: " + token);
                                                    return getWalletBalanceObservable(token);
                                                }
                                            });
                                }
                            }
                            return Observable.error(networkException);
                        }
                        return Observable.error(throwable);
                    }
                })
                .map(new ResponseToWalletBalance())
                .doOnNext(new Action1<Wallet>() {
                    @Override
                    public void call(Wallet wallet) {
                        setWalletBalanceExpireTime();
                    }
                });
    }

    private Observable<Response> getWalletBalanceObservable(final String accessToken ) {
        return localBitcoins.getWalletBalance(accessToken);
    }

    public Observable<JSONObject> validatePinCode(final String pinCode) {
        final String accessToken = AuthUtils.getAccessToken(preference, sharedPreferences);
        return validatePinCodeObservable(accessToken, pinCode)
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                    @Override
                    public Observable<? extends Response> call(Throwable throwable) {
                        NetworkException networkException = null;
                        if (throwable instanceof NetworkException) {
                            networkException = (NetworkException) throwable;
                        }
                        if(networkException != null) {
                            if(networkException.getStatus() == DataServiceUtils.STATUS_403) {
                                Timber.d("refreshTokenAndRetry 403");
                                return refreshTokens()
                                        .flatMap(new Func1<String, Observable<? extends Response>>() {
                                            @Override
                                            public Observable<? extends Response> call(String token) {
                                                Timber.d("new token: " + token);
                                                return validatePinCodeObservable(token, pinCode);
                                            }
                                        });
                            } else if (networkException.getStatus() == DataServiceUtils.STATUS_400) {
                                Timber.d("refreshTokenAndRetry 400");
                                if (networkException.getCode() == DataServiceUtils.CODE_THREE) {
                                    return refreshTokens()
                                            .flatMap(new Func1<String, Observable<? extends Response>>() {
                                                @Override
                                                public Observable<? extends Response> call(String token) {
                                                    Timber.d("new token: " + token);
                                                    return validatePinCodeObservable(token, pinCode);
                                                }
                                            });
                                }
                            }
                            return Observable.error(networkException);
                        }
                        return Observable.error(throwable);
                    }
                })
                .map(new ResponseToJSONObject());
    }

    private Observable<Response> validatePinCodeObservable(final String accessToken, final String pinCode) {
        return localBitcoins.checkPinCode(accessToken, pinCode);
    }

    public Observable<JSONObject> contactAction(final String contactId, final String pinCode, final ContactAction action) {
        final String accessToken = AuthUtils.getAccessToken(preference, sharedPreferences);
        return contactActionObservable(accessToken, contactId, pinCode, action)
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                    @Override
                    public Observable<? extends Response> call(Throwable throwable) {
                        NetworkException networkException = null;
                        if (throwable instanceof NetworkException) {
                            networkException = (NetworkException) throwable;
                        }
                        if(networkException != null) {
                            if(networkException.getStatus() == DataServiceUtils.STATUS_403) {
                                Timber.d("refreshTokenAndRetry 403");
                                return refreshTokens()
                                        .flatMap(new Func1<String, Observable<? extends Response>>() {
                                            @Override
                                            public Observable<? extends Response> call(String token) {
                                                Timber.d("new token: " + token);
                                                return contactActionObservable(token, contactId, pinCode, action);
                                            }
                                        });
                            } else if (networkException.getStatus() == DataServiceUtils.STATUS_400) {
                                Timber.d("refreshTokenAndRetry 400");
                                if (networkException.getCode() == DataServiceUtils.CODE_THREE) {
                                    return refreshTokens()
                                            .flatMap(new Func1<String, Observable<? extends Response>>() {
                                                @Override
                                                public Observable<? extends Response> call(String token) {
                                                    Timber.d("new token: " + token);
                                                    return contactActionObservable(token, contactId, pinCode, action);
                                                }
                                            });
                                }
                            }
                            return Observable.error(networkException);
                        }
                        return Observable.error(throwable);
                    }
                })
                .map(new ResponseToJSONObject());
    }

    private Observable<Response> contactActionObservable(final String accessToken, final String contactId, final String pinCode, final ContactAction action) {
        switch (action) {
            case RELEASE:
                return localBitcoins.releaseContactPinCode(accessToken, contactId, pinCode);
            case CANCEL:
                return localBitcoins.contactCancel(accessToken, contactId);
            case DISPUTE:
                return localBitcoins.contactDispute(accessToken, contactId);
            case PAID:
                return localBitcoins.markAsPaid(accessToken, contactId);
            case FUND:
                return localBitcoins.contactFund(accessToken, contactId);
        }

        return Observable.error(new Error("Unable to perform action on contact"));
    }

    public Observable<JSONObject> updateAdvertisement(final Advertisement advertisement) {
        final String accessToken = AuthUtils.getAccessToken(preference, sharedPreferences);
        return updateAdvertisementObservable(accessToken, advertisement)
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                    @Override
                    public Observable<? extends Response> call(Throwable throwable) {
                        NetworkException networkException = null;
                        if (throwable instanceof NetworkException) {
                            networkException = (NetworkException) throwable;
                        }
                        if(networkException != null) {
                            if(networkException.getStatus() == DataServiceUtils.STATUS_403) {
                                Timber.d("refreshTokenAndRetry 403");
                                return refreshTokens()
                                        .flatMap(new Func1<String, Observable<? extends Response>>() {
                                            @Override
                                            public Observable<? extends Response> call(String token) {
                                                Timber.d("new token: " + token);
                                                return updateAdvertisementObservable(token, advertisement);
                                            }
                                        });
                            } else if (networkException.getStatus() == DataServiceUtils.STATUS_400) {
                                Timber.d("refreshTokenAndRetry 400");
                                if (networkException.getCode() == DataServiceUtils.CODE_THREE) {
                                    return refreshTokens()
                                            .flatMap(new Func1<String, Observable<? extends Response>>() {
                                                @Override
                                                public Observable<? extends Response> call(String token) {
                                                    Timber.d("new token: " + token);
                                                    return updateAdvertisementObservable(token, advertisement);
                                                }
                                            });
                                }
                            }
                            return Observable.error(networkException);
                        }
                        return Observable.error(throwable);
                    }
                })
                .map(new ResponseToJSONObject())
                .flatMap(new Func1<JSONObject, Observable<JSONObject>>() {
                    @Override
                    public Observable<JSONObject> call(JSONObject jsonObject) {
                        return Observable.just(jsonObject);
                    }
                });
    }

    private Observable<Response> updateAdvertisementObservable(final String accessToken, final Advertisement advertisement) {
        final String city;
        if (Strings.isBlank(advertisement.getCity())) {
            city = advertisement.getLocation();
        } else {
            city = advertisement.getCity();
        }
        return localBitcoins.updateAdvertisement(
                accessToken, String.valueOf(advertisement.getAd_id()), advertisement.getAccount_info(), advertisement.getBank_name(), city, advertisement.getCountry_code(), advertisement.getCurrency(),
                String.valueOf(advertisement.getLat()), advertisement.getLocation(), String.valueOf(advertisement.getLon()), advertisement.getMax_amount(), advertisement.getMin_amount(),
                advertisement.getMessage(), advertisement.getPrice_equation(), String.valueOf(advertisement.getTrusted_required()), String.valueOf(advertisement.getSms_verification_required()),
                String.valueOf(advertisement.getTrack_max_amount()), String.valueOf(advertisement.getVisible()), String.valueOf(advertisement.getRequire_identification()),
                advertisement.getRequire_feedback_score(), advertisement.getRequire_trade_volume(), advertisement.getFirst_time_limit_btc(),
                advertisement.getPhone_number(), advertisement.getOpening_hours());
    }

    public Observable<JSONObject> createAdvertisement(final Advertisement advertisement) {
        final String accessToken = AuthUtils.getAccessToken(preference, sharedPreferences);
        return createAdvertisementObservable(accessToken, advertisement)
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                    @Override
                    public Observable<? extends Response> call(Throwable throwable) {
                        NetworkException networkException = null;
                        if (throwable instanceof NetworkException) {
                            networkException = (NetworkException) throwable;
                        }
                        if(networkException != null) {
                            if(networkException.getStatus() == DataServiceUtils.STATUS_403) {
                                Timber.d("refreshTokenAndRetry 403");
                                return refreshTokens()
                                        .flatMap(new Func1<String, Observable<? extends Response>>() {
                                            @Override
                                            public Observable<? extends Response> call(String token) {
                                                Timber.d("new token: " + token);
                                                return createAdvertisementObservable(token, advertisement);
                                            }
                                        });
                            } else if (networkException.getStatus() == DataServiceUtils.STATUS_400) {
                                Timber.d("refreshTokenAndRetry 400");
                                if (networkException.getCode() == DataServiceUtils.CODE_THREE) {
                                    return refreshTokens()
                                            .flatMap(new Func1<String, Observable<? extends Response>>() {
                                                @Override
                                                public Observable<? extends Response> call(String token) {
                                                    Timber.d("new token: " + token);
                                                    return createAdvertisementObservable(token, advertisement);
                                                }
                                            });
                                }
                            }
                            return Observable.error(networkException);
                        }
                        return Observable.error(throwable);
                    }
                })
                .map(new ResponseToJSONObject());
    }

    private Observable<Response> createAdvertisementObservable(final String accessToken, final Advertisement advertisement) {
        String city;
        if (TextUtils.isEmpty(advertisement.getCity())) {
            city = advertisement.getLocation();
        } else {
            city = advertisement.getCity();
        }

        return localBitcoins.createAdvertisement(accessToken, advertisement.getMin_amount(),
                advertisement.getMax_amount(), advertisement.getPrice_equation(), advertisement.getTrade_type(), advertisement.getOnline_provider(),
                String.valueOf(advertisement.getLat()), String.valueOf(advertisement.getLon()),
                city, advertisement.getLocation(), advertisement.getCountry_code(), advertisement.getAccount_info(), advertisement.getBank_name(),
                String.valueOf(advertisement.getSms_verification_required()), String.valueOf(advertisement.getTrack_max_amount()),
                String.valueOf(advertisement.getTrusted_required()), String.valueOf(advertisement.getRequire_identification()),
                advertisement.getRequire_feedback_score(), advertisement.getRequire_trade_volume(),
                advertisement.getFirst_time_limit_btc(), advertisement.getMessage(), advertisement.getCurrency(),
                advertisement.getPhone_number(), advertisement.getOpening_hours());
    }

    public Observable<JSONObject> postMessage(final String contact_id, final String message) {
        final String accessToken = AuthUtils.getAccessToken(preference, sharedPreferences);
        return postMessageObservable(accessToken, contact_id, message)
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                    @Override
                    public Observable<? extends Response> call(Throwable throwable) {
                        NetworkException networkException = null;
                        if (throwable instanceof NetworkException) {
                            networkException = (NetworkException) throwable;
                        }
                        if(networkException != null) {
                            if(networkException.getStatus() == DataServiceUtils.STATUS_403) {
                                Timber.d("refreshTokenAndRetry 403");
                                return refreshTokens()
                                        .flatMap(new Func1<String, Observable<? extends Response>>() {
                                            @Override
                                            public Observable<? extends Response> call(String token) {
                                                Timber.d("new token: " + token);
                                                return postMessageObservable(token, contact_id, message);
                                            }
                                        });
                            } else if (networkException.getStatus() == DataServiceUtils.STATUS_400) {
                                Timber.d("refreshTokenAndRetry 400");
                                if (networkException.getCode() == DataServiceUtils.CODE_THREE) {
                                    return refreshTokens()
                                            .flatMap(new Func1<String, Observable<? extends Response>>() {
                                                @Override
                                                public Observable<? extends Response> call(String token) {
                                                    Timber.d("new token: " + token);
                                                    return postMessageObservable(token, contact_id, message);
                                                }
                                            });
                                }
                            }
                            return Observable.error(networkException);
                        }
                        return Observable.error(throwable);
                    }
                })
                .map(new ResponseToJSONObject());
    }

    public Observable<JSONObject> postMessageWithAttachment(final String contact_id, final String message, final File file) {
        final String accessToken = AuthUtils.getAccessToken(preference, sharedPreferences);
        return postMessageWithAttachmentObservable(accessToken, contact_id, message, file)
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                    @Override
                    public Observable<? extends Response> call(Throwable throwable) {
                        NetworkException networkException = null;
                        if (throwable instanceof NetworkException) {
                            networkException = (NetworkException) throwable;
                        }
                        if(networkException != null) {
                            if(networkException.getStatus() == DataServiceUtils.STATUS_403) {
                                Timber.d("refreshTokenAndRetry 403");
                                return refreshTokens()
                                        .flatMap(new Func1<String, Observable<? extends Response>>() {
                                            @Override
                                            public Observable<? extends Response> call(String token) {
                                                Timber.d("new token: " + token);
                                                return postMessageWithAttachmentObservable(token, contact_id, message, file);
                                            }
                                        });
                            } else if (networkException.getStatus() == DataServiceUtils.STATUS_400) {
                                Timber.d("refreshTokenAndRetry 400");
                                if (networkException.getCode() == DataServiceUtils.CODE_THREE) {
                                    return refreshTokens()
                                            .flatMap(new Func1<String, Observable<? extends Response>>() {
                                                @Override
                                                public Observable<? extends Response> call(String token) {
                                                    Timber.d("new token: " + token);
                                                    return postMessageWithAttachmentObservable(token, contact_id, message, file);
                                                }
                                            });
                                }
                            }
                            return Observable.error(networkException);
                        }
                        return Observable.error(throwable);
                    }
                })
                .map(new ResponseToJSONObject());
    }

    private Observable<Response> postMessageObservable(final String accessToken, final String contact_id, final String message) {
        return localBitcoins.contactMessagePost(accessToken, contact_id, message);
    }

    private Observable<Response> postMessageWithAttachmentObservable(final String accessToken, final String contact_id, final String message, final File file) {
        TypedFile typedFile = new TypedFile("multipart/form-data", file);
        final LinkedHashMap<String, String> params = new LinkedHashMap<String, String>();
        params.put("msg", message);
        return localBitcoins.contactMessagePostWithAttachment(accessToken, contact_id, params, typedFile);
    }

    public Observable<User> getMyself(String accessToken) {
        return localBitcoins.getMyself(accessToken)
                .map(new ResponseToUser());
    }

    public Observable<Contact> getContactInfo(final String contact_id) {
        final String accessToken = AuthUtils.getAccessToken(preference, sharedPreferences);
        return getContactInfoObservable(accessToken, contact_id)
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                    @Override
                    public Observable<? extends Response> call(Throwable throwable) {
                        NetworkException networkException = null;
                        if (throwable instanceof NetworkException) {
                            networkException = (NetworkException) throwable;
                        }
                        if(networkException != null) {
                            if(networkException.getStatus() == DataServiceUtils.STATUS_403) {
                                Timber.d("refreshTokenAndRetry 403");
                                return refreshTokens()
                                        .flatMap(new Func1<String, Observable<? extends Response>>() {
                                            @Override
                                            public Observable<? extends Response> call(String token) {
                                                Timber.d("new token: " + token);
                                                return getContactInfoObservable(token, contact_id);
                                            }
                                        });
                            } else if (networkException.getStatus() == DataServiceUtils.STATUS_400) {
                                Timber.d("refreshTokenAndRetry 400");
                                if (networkException.getCode() == DataServiceUtils.CODE_THREE) {
                                    return refreshTokens()
                                            .flatMap(new Func1<String, Observable<? extends Response>>() {
                                                @Override
                                                public Observable<? extends Response> call(String token) {
                                                    Timber.d("new token: " + token);
                                                    return getContactInfoObservable(token, contact_id);
                                                }
                                            });
                                }
                            }
                            return Observable.error(networkException);
                        }
                        return Observable.error(throwable);
                    }
                })
                .map(new ResponseToContact())
                .flatMap(new Func1<Contact, Observable<? extends Contact>>() {
                    @Override
                    public Observable<? extends Contact> call(final Contact contact) {
                        return getContactMessagesObservable(String.valueOf(contact.getContact_id()))
                                .map(new ResponseToMessages())
                                .map(new Func1<List<Message>, Contact>() {
                                    @Override
                                    public Contact call(List<Message> messages) {
                                        if (messages != null) {
                                            contact.setMessages(messages);
                                        }
                                        return contact;
                                    }
                                });
                    }
                });
    }

    private Observable<Response> getContactInfoObservable(String accessToken, final String contact_id) {
        return localBitcoins.getContactInfo(accessToken, contact_id);
    }

    private Observable<Response> getContactMessagesObservable(final String contact_id) {
        final String accessToken = AuthUtils.getAccessToken(preference, sharedPreferences);
        return localBitcoins.contactMessages(accessToken, contact_id);
    }

    public Observable<List<Notification>> getNotifications() {
        final String accessToken = AuthUtils.getAccessToken(preference, sharedPreferences);
        return getNotificationsObservable(accessToken)
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                    @Override
                    public Observable<? extends Response> call(Throwable throwable) {
                        NetworkException networkException = null;
                        if (throwable instanceof NetworkException) {
                            networkException = (NetworkException) throwable;
                        }
                        if(networkException != null) {
                            if(networkException.getStatus() == DataServiceUtils.STATUS_403) {
                                Timber.d("refreshTokenAndRetry 403");
                                return refreshTokens()
                                        .flatMap(new Func1<String, Observable<? extends Response>>() {
                                            @Override
                                            public Observable<? extends Response> call(String token) {
                                                Timber.d("new token: " + token);
                                                return getNotificationsObservable(token);
                                            }
                                        });
                            } else if (networkException.getStatus() == DataServiceUtils.STATUS_400) {
                                Timber.d("refreshTokenAndRetry 400");
                                if (networkException.getCode() == DataServiceUtils.CODE_THREE) {
                                    return refreshTokens()
                                            .flatMap(new Func1<String, Observable<? extends Response>>() {
                                                @Override
                                                public Observable<? extends Response> call(String token) {
                                                    Timber.d("new token: " + token);
                                                    return getNotificationsObservable(token);
                                                }
                                            });
                                }
                            }
                            return Observable.error(networkException);
                        }
                        return Observable.error(throwable);
                    }
                })
                .map(new ResponseToNotifications());
    }

    private Observable<Response> getNotificationsObservable( String accessToken ) {
        return localBitcoins.getNotifications(accessToken);
    }

    public Observable<JSONObject> markNotificationRead(final String notificationId) {
        final String accessToken = AuthUtils.getAccessToken(preference, sharedPreferences);
        return markNotificationReadObservable(accessToken, notificationId)
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                    @Override
                    public Observable<? extends Response> call(Throwable throwable) {
                        NetworkException networkException = null;
                        if (throwable instanceof NetworkException) {
                            networkException = (NetworkException) throwable;
                        }
                        if(networkException != null) {
                            if(networkException.getStatus() == DataServiceUtils.STATUS_403) {
                                Timber.d("refreshTokenAndRetry 403");
                                return refreshTokens()
                                        .flatMap(new Func1<String, Observable<? extends Response>>() {
                                            @Override
                                            public Observable<? extends Response> call(String token) {
                                                Timber.d("new token: " + token);
                                                return markNotificationReadObservable(token, notificationId);
                                            }
                                        });
                            } else if (networkException.getStatus() == DataServiceUtils.STATUS_400) {
                                Timber.d("refreshTokenAndRetry 400");
                                if (networkException.getCode() == DataServiceUtils.CODE_THREE) {
                                    return refreshTokens()
                                            .flatMap(new Func1<String, Observable<? extends Response>>() {
                                                @Override
                                                public Observable<? extends Response> call(String token) {
                                                    Timber.d("new token: " + token);
                                                    return markNotificationReadObservable(token, notificationId);
                                                }
                                            });
                                }
                            }
                            return Observable.error(networkException);
                        }
                        return Observable.error(throwable);
                    }
                })
                .map(new ResponseToJSONObject());
    }

    private Observable<Response> markNotificationReadObservable(String accessToken, final String notificationId) {
        return localBitcoins.markNotificationRead(accessToken, notificationId);
    }

    public Observable<List<Contact>> getContacts(final DashboardType dashboardType) {
        final String accessToken = AuthUtils.getAccessToken(preference, sharedPreferences);
        switch (dashboardType) {
            case RELEASED:
            case CLOSED:
            case CANCELED:
                return getContactsObservable(accessToken, dashboardType)
                        .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                            @Override
                            public Observable<? extends Response> call(Throwable throwable) {
                                NetworkException networkException = null;
                                if (throwable instanceof NetworkException) {
                                    networkException = (NetworkException) throwable;
                                    throwable = networkException.getCause();
                                }
                                if(networkException != null) {
                                    if(networkException.getStatus() == DataServiceUtils.STATUS_403) {
                                        Timber.d("refreshTokenAndRetry 403");
                                        return refreshTokens()
                                                .flatMap(new Func1<String, Observable<? extends Response>>() {
                                                    @Override
                                                    public Observable<? extends Response> call(String token) {
                                                        Timber.d("new token: " + token);
                                                        return getContactsObservable(token, dashboardType);
                                                    }
                                                });
                                    } else if (networkException.getStatus() == DataServiceUtils.STATUS_400) {
                                        Timber.d("refreshTokenAndRetry 400");
                                        if (networkException.getCode() == DataServiceUtils.CODE_THREE) {
                                            return refreshTokens()
                                                    .flatMap(new Func1<String, Observable<? extends Response>>() {
                                                        @Override
                                                        public Observable<? extends Response> call(String token) {
                                                            Timber.d("new token: " + token);
                                                            return getContactsObservable(token, dashboardType);
                                                        }
                                                    });
                                        }
                                    }
                                    return Observable.error(networkException);
                                }
                                return Observable.error(throwable);
                            }
                        })
                        .map(new ResponseToContacts())
                        .flatMap(new Func1<List<Contact>, Observable<? extends List<Contact>>>() {
                            @Override
                            public Observable<? extends List<Contact>> call(final List<Contact> contacts) {
                                //setContactsExpireTime();
                                return Observable.just(contacts);
                            }
                        });
            default:
                if (!needToRefreshContacts()) {
                    return Observable.just(null);
                }
                return getContactsObservable(accessToken)
                        .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                            @Override
                            public Observable<? extends Response> call(Throwable throwable) {
                                setContactsExpireTime();
                                NetworkException networkException = null;
                                if (throwable instanceof NetworkException) {
                                    networkException = (NetworkException) throwable;
                                }
                                if(networkException != null) {
                                    if(networkException.getStatus() == DataServiceUtils.STATUS_403) {
                                        Timber.d("refreshTokenAndRetry 403");
                                        return refreshTokens()
                                                .flatMap(new Func1<String, Observable<? extends Response>>() {
                                                    @Override
                                                    public Observable<? extends Response> call(String token) {
                                                        Timber.d("new token: " + token);
                                                        return getContactsObservable(token);
                                                    }
                                                });
                                    } else if (networkException.getStatus() == DataServiceUtils.STATUS_400) {
                                        Timber.d("refreshTokenAndRetry 400");
                                        if (networkException.getCode() == DataServiceUtils.CODE_THREE) {
                                            return refreshTokens()
                                                    .flatMap(new Func1<String, Observable<? extends Response>>() {
                                                        @Override
                                                        public Observable<? extends Response> call(String token) {
                                                            Timber.d("new token: " + token);
                                                            return getContactsObservable(token);
                                                        }
                                                    });
                                        }
                                    }
                                    return Observable.error(networkException);
                                }
                                return Observable.error(throwable);
                            }
                        })
                        .map(new ResponseToContacts())
                        .flatMap(new Func1<List<Contact>, Observable<? extends List<Contact>>>() {
                            @Override
                            public Observable<? extends List<Contact>> call(final List<Contact> contacts) {
                                setContactsExpireTime();
                                return Observable.just(contacts);
                            }
                        });
        }
    }

    private Observable<Response> getContactsObservable(String accessToken ) {
        return localBitcoins.getDashboard(accessToken);
    }

    private Observable<Response> getContactsObservable(String accessToken, final DashboardType dashboardType) {
        return localBitcoins.getDashboard(accessToken, dashboardType.name().toLowerCase());
    }

    public Observable<Advertisement> getAdvertisement(final String adId) {
        final String accessToken = AuthUtils.getAccessToken(preference, sharedPreferences);
        return localBitcoins.getAdvertisement(accessToken, adId)
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                    @Override
                    public Observable<? extends Response> call(Throwable throwable) {
                        NetworkException networkException = null;
                        if (throwable instanceof NetworkException) {
                            networkException = (NetworkException) throwable;
                        }
                        if(networkException != null) {
                            if(networkException.getStatus() == DataServiceUtils.STATUS_403) {
                                Timber.d("refreshTokenAndRetry 403");
                                return refreshTokens()
                                        .flatMap(new Func1<String, Observable<? extends Response>>() {
                                            @Override
                                            public Observable<? extends Response> call(String token) {
                                                Timber.d("new token: " + token);
                                                return localBitcoins.getAdvertisement(token, adId);
                                            }
                                        });
                            } else if (networkException.getStatus() == DataServiceUtils.STATUS_400) {
                                Timber.d("refreshTokenAndRetry 400");
                                if (networkException.getCode() == DataServiceUtils.CODE_THREE) {
                                    return refreshTokens()
                                            .flatMap(new Func1<String, Observable<? extends Response>>() {
                                                @Override
                                                public Observable<? extends Response> call(String token) {
                                                    Timber.d("new token: " + token);
                                                    return localBitcoins.getAdvertisement(token, adId);
                                                }
                                            });
                                }
                            }
                            return Observable.error(networkException);
                        }
                        return Observable.error(throwable);
                    }
                })
                .map(new ResponseToAd());
    }

    public Observable<List<Advertisement>> getAdvertisements(boolean force) {
        if (!needToRefreshAdvertisements() && !force) {
            return Observable.just(null);
        }
        final String accessToken = AuthUtils.getAccessToken(preference, sharedPreferences);
        return getAdvertisementsObservable(accessToken)
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                    @Override
                    public Observable<? extends Response> call(Throwable throwable) {
                        NetworkException networkException = null;
                        setAdvertisementsExpireTime();
                        if (throwable instanceof NetworkException) {
                            networkException = (NetworkException) throwable;
                        }
                        if(networkException != null) {
                            if(networkException.getStatus() == DataServiceUtils.STATUS_403) {
                                Timber.d("refreshTokenAndRetry 403");
                                return refreshTokens()
                                        .flatMap(new Func1<String, Observable<? extends Response>>() {
                                            @Override
                                            public Observable<? extends Response> call(String token) {
                                                Timber.d("new token: " + token);
                                                return getAdvertisementsObservable(token);
                                            }
                                        });
                            } else if (networkException.getStatus() == DataServiceUtils.STATUS_400) {
                                Timber.d("refreshTokenAndRetry 400");
                                if (networkException.getCode() == DataServiceUtils.CODE_THREE) {
                                    return refreshTokens()
                                            .flatMap(new Func1<String, Observable<? extends Response>>() {
                                                @Override
                                                public Observable<? extends Response> call(String token) {
                                                    Timber.d("new token: " + token);
                                                    return getAdvertisementsObservable(token);
                                                }
                                            });
                                }
                            }
                            return Observable.error(networkException);
                        }
                        return Observable.error(throwable);
                    }
                })
                .doOnNext(new Action1<Response>() {
                    @Override
                    public void call(Response response) {
                        Timber.d("getAdvertisementsm setAdvertisementsExpireTime");
                        setAdvertisementsExpireTime();
                    }
                })
                .map(new ResponseToAds());
    }

    private Observable<Response> getAdvertisementsObservable(String accessToken) {
        return localBitcoins.getAds(accessToken);
    }

    public Observable<JSONObject> updateAdvertisementVisibility(final Advertisement advertisement, final boolean visible) {
        advertisement.setVisible(visible);
        final String accessToken = AuthUtils.getAccessToken(preference, sharedPreferences);
        return updateAdvertisementObservable(accessToken, advertisement)
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                    @Override
                    public Observable<? extends Response> call(Throwable throwable) {
                        NetworkException networkException = null;
                        if (throwable instanceof NetworkException) {
                            networkException = (NetworkException) throwable;
                        }
                        if(networkException != null) {
                            if(networkException.getStatus() == DataServiceUtils.STATUS_403) {
                                Timber.d("refreshTokenAndRetry 403");
                                return refreshTokens()
                                        .flatMap(new Func1<String, Observable<? extends Response>>() {
                                            @Override
                                            public Observable<? extends Response> call(String token) {
                                                Timber.d("new token: " + token);
                                                return updateAdvertisementObservable(token, advertisement);
                                            }
                                        });
                            } else if (networkException.getStatus() == DataServiceUtils.STATUS_400) {
                                Timber.d("refreshTokenAndRetry 400");
                                if (networkException.getCode() == DataServiceUtils.CODE_THREE) {
                                    return refreshTokens()
                                            .flatMap(new Func1<String, Observable<? extends Response>>() {
                                                @Override
                                                public Observable<? extends Response> call(String token) {
                                                    Timber.d("new token: " + token);
                                                    return updateAdvertisementObservable(token, advertisement);
                                                }
                                            });
                                }
                            }
                            return Observable.error(networkException);
                        }
                        return Observable.error(throwable);
                    }
                })
                .map(new ResponseToJSONObject())
                .flatMap(new Func1<JSONObject, Observable<JSONObject>>() {
                    @Override
                    public Observable<JSONObject> call(JSONObject jsonObject) {
                        if (Parser.containsError(jsonObject)) {
                            RetroError retroError = Parser.parseError(jsonObject);
                            throw new Error(retroError);
                        } else {
                            return Observable.just(jsonObject);
                        }
                    }
                });
    }

    public Observable<Boolean> deleteAdvertisement(final String adId) {
        final String accessToken = AuthUtils.getAccessToken(preference, sharedPreferences);
        return deleteAdvertisementObservable(accessToken, adId)
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                    @Override
                    public Observable<? extends Response> call(Throwable throwable) {
                        NetworkException networkException = null;
                        if (throwable instanceof NetworkException) {
                            networkException = (NetworkException) throwable;
                        }
                        if(networkException != null) {
                            if(networkException.getStatus() == DataServiceUtils.STATUS_403) {
                                Timber.d("refreshTokenAndRetry 403");
                                return refreshTokens()
                                        .flatMap(new Func1<String, Observable<? extends Response>>() {
                                            @Override
                                            public Observable<? extends Response> call(String token) {
                                                Timber.d("new token: " + token);
                                                return deleteAdvertisementObservable(token, adId);
                                            }
                                        });
                            } else if (networkException.getStatus() == DataServiceUtils.STATUS_400) {
                                Timber.d("refreshTokenAndRetry 400");
                                if (networkException.getCode() == DataServiceUtils.CODE_THREE) {
                                    return refreshTokens()
                                            .flatMap(new Func1<String, Observable<? extends Response>>() {
                                                @Override
                                                public Observable<? extends Response> call(String token) {
                                                    Timber.d("new token: " + token);
                                                    return deleteAdvertisementObservable(token, adId);
                                                }
                                            });
                                }
                            }
                            return Observable.error(networkException);
                        }
                        return Observable.error(throwable);
                    }
                })
                .map(new ResponseToJSONObject())
                .flatMap(new Func1<JSONObject, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> call(JSONObject jsonObject) {
                        if (Parser.containsError(jsonObject)) {
                            RetroError retroError = Parser.parseError(jsonObject);
                            throw new Error(retroError);
                        }
                        return Observable.just(true);
                    }
                });
    }

    private Observable<Response> deleteAdvertisementObservable(String accessToken, final String adId) {
        return localBitcoins.deleteAdvertisement(accessToken, adId);
    }

    public Observable<Wallet> getWallet() {
        final String accessToken = AuthUtils.getAccessToken(preference, sharedPreferences);
        return getWalletObservable(accessToken)
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends Response>>() {
                    @Override
                    public Observable<? extends Response> call(Throwable throwable) {
                        NetworkException networkException = null;
                        if (throwable instanceof NetworkException) {
                            networkException = (NetworkException) throwable;
                        }
                        if(networkException != null) {
                            if(networkException.getStatus() == DataServiceUtils.STATUS_403) {
                                Timber.d("refreshTokenAndRetry 403");
                                return refreshTokens()
                                        .flatMap(new Func1<String, Observable<? extends Response>>() {
                                            @Override
                                            public Observable<? extends Response> call(String token) {
                                                Timber.d("new token: " + token);
                                                return getWalletObservable(token);
                                            }
                                        });
                            } else if (networkException.getStatus() == DataServiceUtils.STATUS_400) {
                                Timber.d("refreshTokenAndRetry 403");
                                if (networkException.getCode() == DataServiceUtils.CODE_THREE) {
                                    return refreshTokens()
                                            .flatMap(new Func1<String, Observable<? extends Response>>() {
                                                @Override
                                                public Observable<? extends Response> call(String token) {
                                                    Timber.d("new token: " + token);
                                                    return getWalletObservable(token);
                                                }
                                            });
                                }
                            }
                            return Observable.error(networkException);
                        }
                        return Observable.error(throwable);
                    }
                })
                .map(new ResponseToWallet());
    }

    private Observable<Response> getWalletObservable(String accessToken) {
        return localBitcoins.getWallet(accessToken);
    }

    public Observable<List<Method>> getMethods() {
        return localBitcoins.getOnlineProviders()
                .map(new ResponseToMethod());
    }

    private void resetContactsExpireTime() {
        preference.removePreference(PREFS_CONTACTS_EXPIRE_TIME);
    }

    private void setContactsExpireTime() {
        long expire = System.currentTimeMillis() + CHECK_CONTACTS_DATA; // 1 hours
        preference.putLong(PREFS_CONTACTS_EXPIRE_TIME, expire);
    }

    private void resetExchangeExpireTime() {
        preference.removePreference(PREFS_EXCHANGE_EXPIRE_TIME);
    }

    public boolean needToRefreshMethods() {
        return System.currentTimeMillis() > preference.getLong(PREFS_METHODS_EXPIRE_TIME, -1);
    }

    private void resetMethodsExpireTime() {
        preference.removePreference(PREFS_METHODS_EXPIRE_TIME);
    }

    public void setMethodsExpireTime() {
        long expire = System.currentTimeMillis() + CHECK_METHODS_DATA; // 1 hours
        preference.putLong(PREFS_METHODS_EXPIRE_TIME, expire);
    }

    public boolean needToRefreshCurrency() {
        return System.currentTimeMillis() > preference.getLong(PREFS_CURRENCY_EXPIRE_TIME, -1);
    }

    public void setCurrencyExpireTime() {
        long expire = System.currentTimeMillis() + CHECK_CURRENCY_DATA; // 1 hours
        preference.putLong(PREFS_CURRENCY_EXPIRE_TIME, expire);
    }

    private void resetAdvertisementsExpireTime() {
        preference.removePreference(PREFS_ADVERTISEMENT_EXPIRE_TIME);
    }

    public boolean needToRefreshContacts() {
        return System.currentTimeMillis() > preference.getLong(PREFS_CONTACTS_EXPIRE_TIME, -1);
    }

    private boolean needToRefreshAdvertisements() {
        return System.currentTimeMillis() > preference.getLong(PREFS_ADVERTISEMENT_EXPIRE_TIME, -1);
    }

    private void setAdvertisementsExpireTime() {
        long expire = System.currentTimeMillis() + CHECK_ADVERTISEMENT_DATA; // 1 hour
        preference.putLong(PREFS_ADVERTISEMENT_EXPIRE_TIME, expire);
    }

    private boolean needToRefreshWalletBalance() {
        return System.currentTimeMillis() > preference.getLong(PREFS_WALLET_BALANCE_EXPIRE_TIME, -1);
    }

    private void setWalletBalanceExpireTime() {
        long expire = System.currentTimeMillis() + CHECK_WALLET_BALANCE_DATA; // 1 hours
        preference.putLong(PREFS_WALLET_BALANCE_EXPIRE_TIME, expire);
    }

    private void resetWalletBalanceExpireTime() {
        preference.removePreference(PREFS_WALLET_BALANCE_EXPIRE_TIME);
    }

    public boolean needToRefreshWallet() {
        return System.currentTimeMillis() > preference.getLong(PREFS_WALLET_EXPIRE_TIME, -1);
    }

    public void setWalletExpireTime() {
        long expire = System.currentTimeMillis() + CHECK_WALLET_DATA; // 1 hours
        preference.putLong(PREFS_WALLET_EXPIRE_TIME, expire);
    }

    private void resetWalletExpireTime() {
        preference.removePreference(PREFS_WALLET_EXPIRE_TIME);
    }*/
}