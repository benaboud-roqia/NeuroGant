package com.dianerverotect.billing;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.google.common.collect.ImmutableList;
import com.dianerverotect.PremiumManager;

import java.util.List;

public class BillingManager implements PurchasesUpdatedListener {

    private static final String TAG = "BillingManager";
    private final BillingClient billingClient;
    private final Context context;
    private final BillingListener listener;

    // TODO: Remplacez par vos propres ID de produit définis dans la Google Play Console
    public static final String PREMIUM_MONTHLY_SKU = "premium_monthly";

    public interface BillingListener {
        void onPurchaseSuccess();
        void onPurchaseFailed(String error);
        void onProductsFetched(List<ProductDetails> productDetailsList);
    }

    public BillingManager(Context context, BillingListener listener) {
        this.context = context;
        this.listener = listener;
        this.billingClient = BillingClient.newBuilder(context)
                .setListener(this)
                .enablePendingPurchases()
                .build();
        connectToGooglePlay();
    }

    private void connectToGooglePlay() {
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Connexion à Google Play Billing réussie.");
                    queryAvailableProducts();
                    checkExistingPurchases();
                } else {
                    Log.e(TAG, "Erreur de connexion à Google Play Billing: " + billingResult.getDebugMessage());
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                Log.w(TAG, "Service Google Play Billing déconnecté. Tentative de reconnexion...");
                connectToGooglePlay(); // Tentative de reconnexion
            }
        });
    }

    public void queryAvailableProducts() {
        QueryProductDetailsParams queryProductDetailsParams =
                QueryProductDetailsParams.newBuilder()
                        .setProductList(
                                ImmutableList.of(
                                        QueryProductDetailsParams.Product.newBuilder()
                                                .setProductId(PREMIUM_MONTHLY_SKU)
                                                .setProductType(BillingClient.ProductType.SUBS)
                                                .build()))
                        .build();

        billingClient.queryProductDetailsAsync(
                queryProductDetailsParams,
                (billingResult, productDetailsList) -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && productDetailsList != null) {
                        listener.onProductsFetched(productDetailsList);
                    } else {
                        Log.e(TAG, "Erreur lors de la récupération des produits: " + billingResult.getDebugMessage());
                    }
                });
    }

    public void launchPurchaseFlow(Activity activity, ProductDetails productDetails) {
        if (!billingClient.isReady()) {
            listener.onPurchaseFailed("Client de facturation non prêt.");
            return;
        }

        ImmutableList<BillingFlowParams.ProductDetailsParams> productDetailsParamsList =
                ImmutableList.of(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetails)
                                .setOfferToken(productDetails.getSubscriptionOfferDetails().get(0).getOfferToken())
                                .build()
                );

        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build();

        billingClient.launchBillingFlow(activity, billingFlowParams);
    }

    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase purchase : purchases) {
                handlePurchase(purchase);
            }
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d(TAG, "Achat annulé par l'utilisateur.");
        } else {
            Log.e(TAG, "Erreur d'achat: " + billingResult.getDebugMessage());
            listener.onPurchaseFailed("Erreur d'achat: " + billingResult.getDebugMessage());
        }
    }

    private void handlePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged()) {
                AcknowledgePurchaseParams acknowledgePurchaseParams =
                        AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.getPurchaseToken())
                                .build();
                billingClient.acknowledgePurchase(acknowledgePurchaseParams, billingResult -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        grantPremiumAccess();
                    } else {
                        Log.e(TAG, "Erreur lors de la reconnaissance de l'achat.");
                        listener.onPurchaseFailed("Erreur lors de la finalisation de l'achat.");
                    }
                });
            } else {
                // L'achat a déjà été reconnu, on donne l'accès premium
                grantPremiumAccess();
            }
        }
    }

    private void checkExistingPurchases() {
        billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build(),
                (billingResult, purchases) -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
                        for (Purchase purchase : purchases) {
                            if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED && purchase.isAcknowledged()) {
                                grantPremiumAccess();
                                return; // L'utilisateur est déjà premium
                            }
                        }
                    }
                }
        );
    }

    private void grantPremiumAccess() {
        PremiumManager.getInstance(context).setPremium(true);
        listener.onPurchaseSuccess();
        Log.d(TAG, "Accès Premium accordé.");
        // TODO: Mettre à jour le statut premium sur Firebase
    }

    public void close() {
        if (billingClient != null && billingClient.isReady()) {
            billingClient.endConnection();
        }
    }
} 