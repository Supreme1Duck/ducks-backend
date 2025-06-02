package dto

import com.ducks.dto.ColorDTO
import com.ducks.dto.ShopProductCategoryDTO
import com.ducks.dto.ShopProductChildSizeDTO
import com.ducks.dto.ShopProductDTO
import com.ducks.model.SeasonModel

data class SearchResultDTO(
    val products: List<ShopProductDTO>,
    val sizes: List<ShopProductChildSizeDTO>,
    val categories: List<ShopProductCategoryDTO>,
    val seasons: List<SeasonModel>,
    val colors: List<ColorDTO>,
)
