package com.mmall.service.impl;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.mmall.common.Const;
import com.mmall.common.ResponseCode;
import com.mmall.common.ServerResponse;
import com.mmall.dao.CartMapper;
import com.mmall.dao.ProductMapper;
import com.mmall.pojo.Cart;
import com.mmall.pojo.Product;
import com.mmall.service.ICartService;
import com.mmall.util.BigDecimalUtil;
import com.mmall.util.PropertiesUtil;
import com.mmall.vo.CartProductVo;
import com.mmall.vo.CartVo;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Created by geely
 */
@Service("iCartService")
public class CartServiceImpl implements ICartService {

    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private ProductMapper productMapper;

    public ServerResponse<CartVo> add(Integer userId,Integer productId,Integer count){

        if(productId == null || count == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }

        //先查下这个用户有没有这个商品在购物车里面
        Cart cart = cartMapper.selectCartByUserIdProductId(userId,productId);
        if(cart == null){
            //这个产品不在这个购物车里,需要新增一个这个产品的记录
            Cart cartItem = new Cart();
            cartItem.setQuantity(count);
            //购物车的商品添加进去的时候就是自动勾选的状态
            cartItem.setChecked(Const.Cart.CHECKED);
            cartItem.setProductId(productId);
            cartItem.setUserId(userId);
            cartMapper.insert(cartItem);
        }
        else{
            //这个产品已经在购物车里了.
            //如果产品已存在,数量相加
            count = cart.getQuantity() + count;
            cart.setQuantity(count);
            //更新购物车数量
            cartMapper.updateByPrimaryKeySelective(cart);
        }


        CartVo cartVo= this.getCartVoLimit(userId);
        return ServerResponse.createBySuccess(cartVo);
    }

    public ServerResponse<CartVo> update(Integer userId,Integer productId,Integer count){
        if(productId == null || count == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        Cart cart = cartMapper.selectCartByUserIdProductId(userId,productId);
        if(cart != null){
            cart.setQuantity(count);
        }
        cartMapper.updateByPrimaryKey(cart);
        return this.list(userId);
    }

    public ServerResponse<CartVo> deleteProduct(Integer userId,String productIds){
        List<String> productList = Splitter.on(",").splitToList(productIds);
        if(CollectionUtils.isEmpty(productList)){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        cartMapper.deleteByUserIdProductIds(userId,productList);
        return this.list(userId);
    }


    public ServerResponse<CartVo> list (Integer userId){
        CartVo cartVo = this.getCartVoLimit(userId);
        return ServerResponse.createBySuccess(cartVo);
    }



    public ServerResponse<CartVo> selectOrUnSelect (Integer userId,Integer productId,Integer checked){
        cartMapper.checkedOrUncheckedProduct(userId,productId,checked);
        return this.list(userId);
    }

    public ServerResponse<Integer> getCartProductCount(Integer userId){
        if(userId == null){
            return ServerResponse.createBySuccess(0);
        }
        return ServerResponse.createBySuccess(cartMapper.selectCartProductCount(userId));
    }




    //写一个获得获得购物车订单详情全部显示的方法
    private CartVo getCartVoLimit(Integer userId){
        CartVo cartVo = new CartVo();
        //先根据用户id查到这个用户购物车的商品集合
        List<Cart> cartList = cartMapper.selectCartByUserId(userId);

        List<CartProductVo> cartProductVoList = Lists.newArrayList();
        //初始化下订单总价
        BigDecimal cartTotalPrice = new BigDecimal("0");

        //假如查询到的购物车不为空，那么把查询到的数据组装到volist里面
        if(CollectionUtils.isNotEmpty(cartList)){
            for(Cart cartItem : cartList){
                CartProductVo cartProductVo = new CartProductVo();
                cartProductVo.setId(cartItem.getId());
                cartProductVo.setUserId(userId);
                cartProductVo.setProductId(cartItem.getProductId());

                //根据购物车的每个商品id，查询这个商品在数据库的信息
                Product product = productMapper.selectByPrimaryKey(cartItem.getProductId());
                if(product != null){
                    //然后假如这个产品是存在的，就继续组装这个商品的vo
                    //添加商品主图
                    cartProductVo.setProductMainImage(product.getMainImage());
                    cartProductVo.setProductName(product.getName());
                    //副标题
                    cartProductVo.setProductSubtitle(product.getSubtitle());
                    //状态
                    cartProductVo.setProductStatus(product.getStatus());
                    //价格
                    cartProductVo.setProductPrice(product.getPrice());
                    //库存
                    cartProductVo.setProductStock(product.getStock());


                    //判断库存，设置一个购买数量值，初始化为0
                    int buyLimitCount = 0;
                    //判断商品库存是不是大于等于我们购物车里面的库存
                    if(product.getStock() >= cartItem.getQuantity()){
                        //库存充足的时候,直接把我们购物车的值，给他设置到这个购买数量里面
                        buyLimitCount = cartItem.getQuantity();
                        //这个数量假如够了，我们就这是这个数量是限制成功的值
                        cartProductVo.setLimitQuantity(Const.Cart.LIMIT_NUM_SUCCESS);
                    }else{
                        //假如购物车下单数量大于系统库存，那么我们就直接把系统库存的数量给他，最多也只能买那么多数量了
                        buyLimitCount = product.getStock();
                        //这里给他设置一个限制失败
                        cartProductVo.setLimitQuantity(Const.Cart.LIMIT_NUM_FAIL);
                        //然后购物车中需要更新有效库存,把我们上面得到的能够购买的最大数量给他放到购物车里面去
                        //new一个购物车属性
                        Cart cartForQuantity = new Cart();
                        //所以我们要重新更新购物车里面的数量，就需要重新把这个数量给他存到数据库的购物车里面去
                        //把这条商品项目的id给他拿到，然后数量也给他拿到，然后放进去数据库里面更新为最新的购买数量
                        cartForQuantity.setId(cartItem.getId());
                        cartForQuantity.setQuantity(buyLimitCount);
                        //去数据库更新
                        cartMapper.updateByPrimaryKeySelective(cartForQuantity);
                    }


                    //然后vo的拼装就直接输入我们上面的限制数量了，不需要重新再去数据库查了
                    cartProductVo.setQuantity(buyLimitCount);
                    //计算这件商品乘以数量的总价，这就是购物车中，某件商品的总价
                    cartProductVo.setProductTotalPrice(BigDecimalUtil.mul(product.getPrice().doubleValue(),cartProductVo.getQuantity()));
                    //这一项为勾选状态
                    cartProductVo.setProductChecked(cartItem.getChecked());
                }

                if(cartItem.getChecked() == Const.Cart.CHECKED){
                    //如果商品是已经被勾选,增加到整个的购物车总价中
                    cartTotalPrice = BigDecimalUtil.add(cartTotalPrice.doubleValue(),cartProductVo.getProductTotalPrice().doubleValue());
                }
                //最后把拼接好的一条商品项目给他放到list集合里面
                cartProductVoList.add(cartProductVo);
            }
        }

        //上面全部循环完了,我们可以得到多条商品的显示数据，都在cartProductVoList里面
        //然后就组装最后的购物车订单详情,来进行返回
        //勾选商品的总价
        cartVo.setCartTotalPrice(cartTotalPrice);
        cartVo.setCartProductVoList(cartProductVoList);
        //检查这个用户购物车里面的商品是不是全选,返回值是一个布尔，全选返回true
        //给这个字段给前端，前端才知道是不是全选状态，那么需要把全部商品都勾选上
        //因为我们不可能让前端去遍历商品，再去判断是不是要勾选上

        //把购物车图片的host前缀也给他添加上
        cartVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix"));

        return cartVo;
    }

    private boolean getAllCheckedStatus(Integer userId){
        if(userId == null){
            return false;
        }
        //这条sql直接判断购物车是不是有没有被勾选的，假如有，就返回数量值，那么这个数量只就不等于0，说明有未勾选的商品，返回false
        //假如等于0，说明全部勾选上了，返回true
        return cartMapper.selectCartProductCheckedStatusByUserId(userId) == 0;

    }





























}
